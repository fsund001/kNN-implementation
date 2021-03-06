import java.util.*; 
import java.util.Scanner;
//import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;

public class KNN {
	File file;
	File output;

	//Matrices that stores the different dataSets
	String [][] trainingData;
	String [][] testData;

	String classifier = "<=50K";

	Scanner readFile;
	FileWriter writer;
	
	int columns;
	//Variable that can regulate how many records that will be imported
	int maxRecords=100; //12.06

	//Final k to be used on the test data set
	int recommended_k;
	int labelColumn = 14;	
	int foldColumn = 15;
	int maxKvalue = 39;
	double tp = 0; //true positive
	double tn = 0; //true negative
	double fp = 0; //false positive
	double fn = 0; //false negatie
	double precision = 0;
	double recall = 0;
	//Stores the neigbours of current row
	HashMap<Double, Integer> neighbours;
	ArrayList<Integer> columnsWithMissingValues;

	public KNN(){

		long startTime = System.currentTimeMillis();
		System.out.println("Importing dataset trainingData...please wait");
		trainingData = importData("adult.train.5fold.csv");
		System.out.println("Imported dataset trainingData");
		
		System.out.println(columnsWithMissingValues);
		
		//System.out.println("Imported dataset testData");
		System.out.println("Replacing replaceMissingValues...please wait");
		replaceMissingValues(1,trainingData);
		replaceMissingValues(6,trainingData);
		replaceMissingValues(13,trainingData);
		System.out.println("replaceMissingValues on trainingData DONE");


		System.out.println("Normalising data...please wait");
		
		//0,2,4,10,10,12 are the columns with numerical data
		normalise(0,trainingData);
		normalise(2,trainingData);
		normalise(4,trainingData);
		normalise(10,trainingData);
		normalise(11,trainingData);
		normalise(12,trainingData);
		System.out.println("Normalised attributes on trainingData DONE");
		long currentTime   = System.currentTimeMillis(); //Debug
		double formattingTime = (double)(currentTime - startTime)/1000;
		System.out.println("Formatting completed in: " + formattingTime);
		
		System.out.println("Cross validation in progress...please wait");
		fiveCV(5,trainingData);
		System.out.println("Cross validation completed!");
		
		currentTime   = System.currentTimeMillis();//Debug

		double validationTime = (double)(currentTime - startTime)/1000;//Debug
		System.out.println("Cross validation completed in: " + validationTime);
		
		System.out.println("Importing test data...please wait");
		testData = importData("adult.test.csv");
		System.out.println("Imported testData");
		
		System.out.println("Replacing replaceMissingValues...please wait");		
		replaceMissingValues(1,testData);
		replaceMissingValues(6,testData);
		replaceMissingValues(13,testData);
		System.out.println("replaceMissingValues on testData DONE");

		System.out.println("Normalising data...please wait");
		normalise(0,testData);
		normalise(2,testData);
		normalise(4,testData);
		normalise(10,testData);
		normalise(11,testData);
		normalise(12,testData);
		System.out.println("Normalised attributes on trainingData DONE");

		//Applies the model on the test data with best k
		knnModel(testData, trainingData, recommended_k);
		
		printConfusionMatrix(); //Print the confusion matrix in the terminal
		precision = calculatePrecision();
		System.out.println("Precision: " + precision);

		recall = calculateRecall();
		System.out.println("Recall: " + recall);
		
		long endTime   = System.currentTimeMillis();//Debug
		double totalTime = (double)(endTime - startTime)/1000;//Debug
		System.out.println("Process completed in: " + totalTime);
		
	}

	//Imports data from a csv file
	private String [][] importData(String fileName){
		file = new File(fileName);
		
		
		//Read file to Scanner String
		try {
			readFile = new Scanner(file);
			return buildDataMatrix(readFile);
		}
		catch(IOException exception){
			System.out.println(exception.toString());
		}
		readFile.close();
		return null;
	}

	//This method store the data set in a matrix
	private String [][] buildDataMatrix(Scanner dataSet){
		String [][] dataMatrix = new String[0][0];
		String [][] tempDataMatrix; //Used
		String [] currentRow; //Store the current record from csv file
		//Counter
		int loadedRecords = 0;
		columns = 0;
		//Stores columns index if it's missing a value
		columnsWithMissingValues = new ArrayList<Integer>();

		while(dataSet.hasNextLine() && loadedRecords<maxRecords){
			//Set the num of columns for the matrix
			currentRow = dataSet.nextLine().split(",");
			//If no data has been imported
			if(columns == 0){
				columns = currentRow.length;
				//System.out.println("nr of columns: " + columns);
				//create matrix
				dataMatrix = new String [dataMatrix.length][columns];
			}

			//Create a new expanded matrix with space for new record
			tempDataMatrix = new String [dataMatrix.length+1][columns];
			
	
			//copy data to the temporary matrix
			for (int i = 0; i < dataMatrix.length; i++){
            	for (int j = 0; j < columns; j++){
            		tempDataMatrix[i][j] = dataMatrix[i][j];
            	}
        	}
        
       		//Add new data to the last empty row
        	for(int k=0; k<columns;k++){
        		tempDataMatrix[dataMatrix.length][k]=currentRow[k].replaceAll("\\s","");//Ignores whitespace
        		//DEBUG//System.out.println("Added data to temp: " + tempData[data.length][k]);
        		
        		//Used to find out which columns where missing values
        		if(tempDataMatrix[dataMatrix.length][k].equals("?")){
        			if(!columnsWithMissingValues.contains(k)){
        				columnsWithMissingValues.add(k);
        				//System.out.println("Missing value on column: " + k);
        			}
        		}
        		
        	}
        	
        	//expand the matrix and store the new matrix
        	dataMatrix = new String [dataMatrix.length][columns];
        	dataMatrix=tempDataMatrix;
        	
        	loadedRecords++;
		}
		return dataMatrix;
	}

	//performed on 1, 6, 13 
	private void replaceMissingValues(int column, String [][] dataSet){
			String attributeName = findMostCommonAttr(column, dataSet);
		    for(int k=1; k<dataSet.length;k++){
		    	
        		if(dataSet[k][column].equals("?")){
        				//System.out.println("Before: " + dataSet[k][column]);
        				trainingData[k][column] = attributeName;
        				//System.out.println("Replaced: " + dataSet[k][column] + " " + column + " " + k);
        			
        		}
        	}
	}

	//performed on 1,3,5,6,7,8,9 and 13
	private String findMostCommonAttr(int column, String [][] dataSet){
		//Store attribute names from a column and how amny times it occurs
		HashMap<String, Integer> attributeSet = new HashMap<String, Integer>();
		ArrayList<String> attributeNames = new ArrayList<String>();
		
		String mostCommonAttr = "";
		String temp = "";
		int biggestKey=0;
		//i = 0 to ignore the header row in the data set
		for (int i=1; i<dataSet.length; i++) {
			if(attributeSet.containsKey(dataSet[i][column])){
				attributeSet.put(dataSet[i][column], attributeSet.get(dataSet[i][column]) + 1);
			}
			else{
				attributeSet.put(dataSet[i][column], 1);
				attributeNames.add(dataSet[i][column]);
			}
		}

		for(int j = 0; j<attributeNames.size(); j++){
			if (attributeSet.get(attributeNames.get(j))>biggestKey){
				biggestKey = attributeSet.get(attributeNames.get(j));
				mostCommonAttr = attributeNames.get(j);
			}	
		}

		//System.out.println(mostCommonAttr);
		return mostCommonAttr;

	}

	/* Never used
	//performed on 0,2,4,10,10,12
	private void findMedian(int columnNum, String [][] dataSet){
		ArrayList<Double> column = new ArrayList<Double>();
		for (int j = 1; j < dataSet.length; j++){
					double number = Double.parseDouble(dataSet[j][columnNum]);
				
					System.out.println(number);
					if(!column.contains(number)){
						column.add(number);
					}
            		
		}
		Collections.sort(column);
		double median = column.get(dataSet.length/column.size());
		//System.out.println(list);
		System.out.println(median);
	}
	*/

	//Identifies the minimum value in a column
	private double getMin(int columnNum, String [][] dataSet){
		ArrayList<Double> column = new ArrayList<Double>();
		//System.out.println(dataSet.length);
		for (int j = 1; j < dataSet.length; j++){
			double number = Double.parseDouble(dataSet[j][columnNum]);
			
			column.add(number);
		}
		Collections.sort(column);
		//System.out.println("Min: " + column.get(0));
		return column.get(0);
	}

	//Identifies the maximum value in a colum
	private double getMax(int columnNum, String [][] dataSet){
		ArrayList<Double> column = new ArrayList<Double>();
		for (int j = 1; j < dataSet.length; j++){
			double number = Double.parseDouble(dataSet[j][columnNum]);
		
			column.add(number);
		}
		Collections.sort(column);
		//System.out.println("Max: " + column.get(column.size()-1) + " at columnNum: " + columnNum);
		return column.get(column.size()-1);

	}

	private void normalise(int column, String [][] dataSet){
		double min = getMin(column, dataSet);
		double max = getMax(column, dataSet);
		double normValue;
		double originalValue;
		//To avoid division with zero
		if(min != max){
			for(int i = 1; i <dataSet.length; i++){
				originalValue = Double.parseDouble(dataSet[i][column]);

				normValue = Math.abs((originalValue - min)/(max - min));
			
				dataSet[i][column] = String.valueOf(normValue);
				//System.out.println("Normalised " + originalValue + " too " + dataSet[i][column]);
			}
		}
	}

	// KNN method used for cross validation
	private String k_NN(int k, int row, String[][] testData, String[][] trainingData, Iterator <Integer> iterator){
		//System.out.println("kayNN method:-----------------------");
	
		String prediction;
		//Stores Distance as Key, and index as Value
		neighbours = new HashMap<Double, Integer>();
		
		ArrayList<Double> neighbourDistances = new ArrayList<Double>();
		//ArrayList<String> neighbourLabels = new ArrayList<String>();
		
		//for (int i = 1; i < trainingData.length; i++){
		while(iterator.hasNext()){
			int currentIndex = iterator.next();
			double distance = euclideanDistance(row, currentIndex, testData, trainingData);
			//If max number of neighborsnot reach
			if(neighbourDistances.size()<k){
				neighbourDistances.add(distance);
				neighbours.put(distance, currentIndex);
				Collections.sort(neighbourDistances);
			}	
			else{
				//If Max number of neigbors reached and current distance is lower the the highest value in list
				if(distance < neighbourDistances.get(neighbourDistances.size()-1)){
					neighbours.remove(neighbourDistances.get(neighbourDistances.size()-1));
					neighbours.put(distance, currentIndex);
					neighbourDistances.remove(neighbourDistances.size()-1);
					neighbourDistances.add(distance);
					Collections.sort(neighbourDistances);
				}
			}
		}

		/* Debug
		System.out.println("Distances list" + neighbourDistances);
		System.out.println("neighbours list" + neighbours);
		System.out.println("neighbours index" + neighbours.get(neighbourDistances.get(neighbourDistances.size()-1)));
		System.out.println(neighbours.values());
		System.out.println(neighbours.values());
		*/
		prediction = labelMaker(row);
		return prediction;

	}

	//For model
	private String k_NN(int k, int row, String[][] testData, String[][] trainingData){
		//System.out.println("kayNN method:-----------------------");
		String prediction;
		//Stores Distance as Key, and index as Value
		neighbours = new HashMap<Double, Integer>();
		
		ArrayList<Double> neighbourDistances = new ArrayList<Double>();
		//ArrayList<String> neighbourLabels = new ArrayList<String>();
		
		for (int i = 1; i < trainingData.length; i++){
			double distance = euclideanDistance(row, i, testData, trainingData);
			
			//If max number of neighborsnot reach
			if(neighbourDistances.size()<k){
				neighbourDistances.add(distance);
				neighbours.put(distance, i);
				Collections.sort(neighbourDistances);
			}	
			else{
				//If Max number of neigbors reached and current distance is lower the the highest value in list
				if(distance < neighbourDistances.get(neighbourDistances.size()-1)){
					//System.out.println("Adding" + distance + " and " + currentIndex);
					neighbours.remove(neighbourDistances.get(neighbourDistances.size()-1));
					neighbours.put(distance, i);
					neighbourDistances.remove(neighbourDistances.size()-1);
					neighbourDistances.add(distance);
					Collections.sort(neighbourDistances);
				}
			}
		}

		prediction = labelMaker(row);
		return prediction;

	}

	//returns predicted label
	private String labelMaker(int row){
		//System.out.println("labelMaker");
		HashMap<String, Integer> predictedEarnings = new HashMap<String, Integer>();
		ArrayList<String> label = new ArrayList<String>();
		//stores the indices of the neighbours
		Iterator <Integer> iterator = neighbours.values().iterator();
		String prediction = "";
		
		//count all the different labels among the neigbors
		while(iterator.hasNext()){
			int index = iterator.next();

			prediction = trainingData[index][labelColumn];
			
			if(!predictedEarnings.containsKey(prediction)){
				predictedEarnings.put(trainingData[index][labelColumn], 1);
			}
			else{
				predictedEarnings.put(trainingData[index][labelColumn], predictedEarnings.get(trainingData[index][labelColumn]) + 1 );
				label.add(trainingData[index][labelColumn]);
			}
			
			
		}

		//Find the most frequent label
		int maxValue = Collections.max(predictedEarnings.values());
		for(int j = 0; j<label.size(); j++){
			if (predictedEarnings.get(label.get(j)) == maxValue ){
				prediction = label.get(j);
			}	
		}

		return prediction;
	}

	//calculates the distance between two rows
	private double euclideanDistance(int row, int row2, String [][] testFold, String[][] trainingData){
		//System.out.println("Euclidean method: ---------------------- ");
		double distance = 0;
		for (int i = 0; i < 14; i++){
			//System.out.println("Euclidean i " + i);
			//System.out.println("Euclidean p1: " + testFold[row][i]);
			//System.out.println("Euclidean p2: " + trainingData[row2][i]);
			
			//try to perform a double operation
			try{
				double dp1 = Double.parseDouble(testFold[row][i]);
				double dp2 = Double.parseDouble(trainingData[row2][i]);
				distance = distance + Math.pow(dp1 - dp2, 2.0);
				//System.out.println("numerical distance: " + Math.pow(dp1 - dp2, 2.0));
			}
			catch(Exception e){

				if(testFold[row][i].equals(trainingData[row2][i])){
					distance++;
				}
			}
		}
		
		return distance;
	}

	//Five fold corss validation method
	private void fiveCV(int folds, String [][] dataSet){
		ArrayList<Integer> current_fold = new ArrayList<Integer>();
		ArrayList<Integer> training_fold;
		ArrayList<Double> k_accuracy = new ArrayList<Double>();
		ArrayList<Integer> valuesOf_k = new ArrayList<Integer>();
		double predictedRight = 0;
		double accuracy;
		for(int i = 1; i<maxKvalue; i= i+2){
			valuesOf_k.add(i);
		}
		
		Iterator <Integer> k_iterator = valuesOf_k.iterator();

		while(k_iterator.hasNext()){
			int k = k_iterator.next();
			System.out.println("K is: " + k);
			
			predictedRight=0;
			accuracy=0;

			for(int fold=1; fold <= folds; fold++){
				System.out.println(" FOLD: " + fold);
				current_fold = new ArrayList<Integer>();
				training_fold = new ArrayList<Integer>();
				for (int i = 1; i < dataSet.length; i++){
					if(dataSet[i][foldColumn].equals(Integer.toString(fold))){
						current_fold.add(i);
					}
					else{
						training_fold.add(i);
					}
				}

				/* Debug
				//System.out.println("Fold " + fold + " length: " + current_fold.size());
				//System.out.println("Fold " + fold + " length: " + current_fold);
				//System.out.println("Fold " + fold + " length: " + training_fold.size());
				//System.out.println("Fold " + fold + " length: " + training_fold);
				*/

				Iterator <Integer> iterator = current_fold.iterator();
				while(iterator.hasNext() && current_fold.size() != 0){
					Iterator <Integer> iterator2 = training_fold.iterator();
					int testRow = iterator.next();

					if(dataSet[testRow][labelColumn].equals(k_NN(k ,testRow, dataSet, dataSet, iterator2))){
						predictedRight++;
						//confusionMatrix(kayNN(k ,x, dataSet, dataSet), dataSet[x][14]);
						//System.out.println("You're the best");
					}
					else{
						//confusionMatrix(kayNN(k ,x, dataSet, dataSet), dataSet[x][14]);
						//System.out.println("Wrong");
					}
				}
			}
			//Accuracy = number true positives / current_fold.size()
			//System.out.println(current_fold.size());
			accuracy=predictedRight/dataSet.length;
			k_accuracy.add(accuracy);
			System.out.println("Accuracy: " + accuracy);
		}

		//Get best k
		double maxi = Collections.max(k_accuracy);
		int ind = k_accuracy.indexOf(maxi);
		int bestKay = valuesOf_k.get(ind);
		System.out.println("Accuracies: " + k_accuracy);
		System.out.println("K: " + bestKay + " accuracy: " + maxi);
		writeToFile(valuesOf_k, k_accuracy, maxi, bestKay);
		recommended_k = bestKay;
	}

	//Counts the different predeictions
	private void confusionMatrix(String predicted, String actual){
		if(predicted.equals(classifier)){
			if(predicted.equals(actual)){
				tp++;
			}
			else{
				fp++;
			}
		}
		else{
			if(predicted.equals(actual)){
				tn++;
			}
			else{
				fn++;
			}
		}

	}

	//Prints a confusion matrix to the terminal
	private void printConfusionMatrix(){
	
		//System.out.println("TP: " + precision);
		String truePos = String.format("%2s", tp).replace(' ', ' ');
		String falsePos = String.format("%2s", fp).replace(' ', ' ');
		String trueNeg = String.format("%2s", tn).replace(' ', ' ');
		String falseNeg  = String.format("%2s", fn).replace(' ', ' ');

		System.out.println();
		System.out.println("Confusion Matrix");
		System.out.print(String.format("%4s", truePos).replace(' ', ' '));
		System.out.print(" |");
		System.out.println(String.format("%4s", falsePos).replace(' ', ' '));
		System.out.print(String.format("%4s", falseNeg).replace(' ', ' '));
		System.out.print(" |");
		System.out.println(String.format("%4s", trueNeg).replace(' ', ' '));
	}


	private double calculatePrecision(){
		//true positive / true positiv + false positive
		return tp/(tp+fp);
	}
	private double calculateRecall(){
		//true positive / true positive + false negative
		return tp/(tp+fn);
	}

	//Writes  K accuracies to file
	private void writeToFile(ArrayList<Integer> ks, ArrayList<Double> accuracy_list, double bestAccuracy, int bestK){
		output = new File("grid.results.txt");
		
		try{
			output.createNewFile();
			writer = new FileWriter(output);
			writer.write("Performance: 5 Cross validation ");
			writer.write("\r\n");
			writer.write("    K   |   A   ");
			writer.write("\r\n");
			for(int i = 0; i<ks.size(); i++){
				//for(int j = 0; j<1; j ++){
				//String.format("%8s", ks.get(i).toString()).replace(' ', '0');
				writer.write(" | ");
				writer.write(String.format("%4s", ks.get(i).toString()).replace(' ', ' '));
				writer.write(" | ");
				writer.write(String.format("%4s", accuracy_list.get(i).toString()).replace(' ', ' '));
				//writer.write(accuracy_list.get(i).toString());
				writer.write(" | ");
				
				//}
				writer.write("\r\n");

			}
			
			writer.write("\r\n");
			writer.write("Best K/A: ");
			writer.write(String.valueOf(bestK));
			writer.write("/");
			writer.write(String.valueOf(bestAccuracy));
			writer.write("\r\n");
			writer.write("K = value of K");
			writer.write("\r\n");
			writer.write("A = accuracy");
			writer.flush();
      		writer.close();
		}
		catch(Exception e){
			System.out.println("Output error: " + e);
		}


	}

	//Performs K NN with recommended best k on test data
	private void knnModel(String[][] testData, String [][] trainingData, int k){
		//Debug//System.out.println(testData.length);
		//Debug//System.out.println(trainingData.length);
		
		System.out.println("KNN model.. please wait");
		System.out.println("Using K value: " + k);
		for(int i = 1; i<testData.length;i++){
				String predicted = k_NN(k, i, testData, trainingData);
				confusionMatrix(predicted, testData[i][14]);
				
				//Used for debug
				//System.out.println(predicted + " was " + testData[i][14]);
			
		}
	}

	public static void main(String args[]){
		new KNN();	
	}

}

