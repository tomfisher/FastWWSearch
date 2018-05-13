package demo;

import items.ExperimentsLauncher;
import tools.UCRArchive;
import weka.classifiers.Evaluation;
import weka.core.Instances;
import windowSearcher.FastWWS;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class UCR_BestWW {
    private static String osName, datasetName, username, projectPath, datasetPath, resDir, sampleType, method;
    private static int bestWarpingWindow;
    private static double bestScore;
    private static int nbRuns = 1;

    public static void main(String[] args) throws Exception {
        // Initialise
        sampleType = "Sorted";				// Doing just 1 dataset, can be Sorted, Small, New or All
        datasetName = "ItalyPowerDemand";	// Name of dataset to be tested
        method = "FastWWSearch";				// Method type in finding the best window

        // Get project and dataset path
        osName = System.getProperty("os.name");
        username = System.getProperty("user.name");
        if (osName.contains("Window")) {
            projectPath = "C:/Users/" + username + "/workspace/FastWWSearch/";
            if (sampleType.equals("New"))
                datasetPath = "C:/Users/" + username + "/workspace/Dataset/TSC_Problems/";
            else
                datasetPath = "C:/Users/" + username + "/workspace/Dataset/UCR_Time_Series_Archive/";
        } else {
            projectPath = "/home/" + username + "/workspace/FastWWSearch/";
            if (sampleType.equals("New"))
                datasetPath = "/home/" + username + "/workspace/Dataset/TSC_Problems/";
            else
                datasetPath = "/home/" + username + "/workspace/Dataset/UCR_Time_Series_Archive/";
        }

        // Get arguments
        if (args.length >= 1) projectPath = args[0];
        if (args.length >= 2) datasetPath = args[1];
        if (args.length >= 3) sampleType = args[2];
        if (sampleType.equals("Single") && args.length >= 4) {
            datasetName = args[3];
            if (args.length >= 5) nbRuns = Integer.parseInt(args[4]);
        } else if (args.length >= 4) {
            nbRuns = Integer.parseInt(args[3]);
        }

        if (sampleType.equals("Single"))
            System.out.println("Find best warping window with " + method + " on " + datasetName + " dataset -- " + nbRuns + " runs");
        else
            System.out.println("Find best warping window with " + method + " on " + sampleType + " dataset -- " + nbRuns + " runs");

        // Run the experiment depending on the given type
        switch(sampleType) {
            case "Sorted":
                for (int j = 0; j < UCRArchive.sortedDataset.length; j++) {
                    datasetName = UCRArchive.sortedDataset[j];
                    singleProblem(datasetName);
                }
                break;
            case "Small":
                for (int j = 0; j < UCRArchive.smallDataset.length; j++) {
                    datasetName = UCRArchive.smallDataset[j];
                    singleProblem(datasetName);
                }
                break;
            case "New":
                for (int j = 0; j < UCRArchive.newTSCProblems.length; j++) {
                    datasetName = UCRArchive.newTSCProblems[j];
                    singleProblem(datasetName);
                }
                break;
            case "All":
                File rep = new File(datasetPath);
                File[] listData = rep.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory();
                    }
                });
                Arrays.sort(listData);

                for (File dataRep : listData) {
                    datasetName = dataRep.getName();
                    singleProblem(datasetName);
                }
                break;
            case "Single":
                singleProblem(datasetName);
                break;
        }
    }// End main

    /**
     * Running the experiment for a single dataset
     * @param datasetName
     * @throws Exception
     */
    private static void singleProblem (String datasetName) throws Exception {
        // Setting output directory
        resDir = projectPath + "outputs/UCR_BestWW/";

        // Check if it exist, else create the directory
        File dir = new File(resDir);
        if (!dir.exists())
            dir.mkdirs();

        // Reading the dataset
        System.out.println("Processing: " + datasetName);
        Instances[] data = ExperimentsLauncher.readTrainAndTest(datasetPath, datasetName);

        Instances train = data[0];
        Instances test = data[1];

        System.out.println("Launching " + method);
        FastWWS classifier = new FastWWS(datasetName);
        classifier.setResDir(resDir);
        classifier.setType(method);

        // Training the classifier for best window
        long start = System.nanoTime();
        classifier.buildClassifier(train);
        long stop = System.nanoTime();
        double searchTime = (stop - start)/1e9;

        bestWarpingWindow = classifier.getBestWin();
        bestScore = classifier.getBestScore();

        System.out.println(searchTime + " s, best WW " + bestWarpingWindow + ", best Score " + bestScore);

        // Evaluate the trained classfier with test set
        Evaluation eval = new Evaluation(train);
        eval.evaluateModel(classifier, test);
        System.out.println(eval.errorRate());

        // Save result
        saveSearchTime(searchTime, eval.errorRate());
    }

    /**
     * Save results (search time) to csv
     * @param searchTime
     * @param error
     */
    private static void saveSearchTime(double searchTime, double error) {
        String fileName = resDir + "UCR_BestWW.csv";
        FileWriter out;
        boolean append = false;
        File file = new File(fileName);
        if (file.exists())
            append = true;
        try {
            out = new FileWriter(fileName, append);
            if (!append)
                out.append("Dataset,SearchTime(s),BestWin,Train Error,Test Error\n");
            out.append(datasetName + "," + searchTime + "," + bestWarpingWindow + "," + bestScore + "," + error + "\n");
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
