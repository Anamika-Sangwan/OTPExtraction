import java.io.*;
import java.util.*;

public class Utility {

    private static final Set<Character> charsWithQuoteSet = new HashSet<>(Arrays.asList(',', '\n', '\r', '\"'));

    public Utility() {
    }

    public String[] readFileData() {
        List<List<String>> dataRows = new ArrayList<>();
        String filePath = getMasterDataFilePath();

        if (filePath != null && !filePath.isEmpty() && new File(filePath).exists()) {
            try (BufferedReader csvReader = new BufferedReader(new FileReader(filePath))) {
                csvReader.readLine();

                List<String> columnValues = new ArrayList<>();
                while (tryGetDataRow(csvReader, true, columnValues)) {
                    columnValues.remove(columnValues.size() - 1);
                    dataRows.add(new ArrayList<>(columnValues));
                    columnValues.clear();
                }
            } catch (IOException e) {
                System.out.println("Error reading CSV file: " + e.getMessage());
            }
        }

        return dataRows.stream()
                .flatMap(List::stream)
                .toArray(String[]::new);
    }

    public static boolean tryGetDataRow(BufferedReader csvReader, boolean skipEmptyRows, List<String> columnValues)
            throws IOException {
        columnValues.clear();

        if (!csvReader.ready()) {
            return false;
        }

        boolean isCurrentRowEmpty;
        do {
            columnValues.clear();
            boolean openingQuote = false;
            StringBuilder sb = new StringBuilder();
            int intChar;

            while ((intChar = csvReader.read()) != -1) {
                char currentChar = (char) intChar;

                if (currentChar == '\"') {
                    openingQuote = !openingQuote;
                }

                if (!openingQuote && currentChar == ',') {
                    columnValues.add(sb.toString());
                    sb.setLength(0);
                } else if (!openingQuote && (currentChar == '\n' || currentChar == '\r')) {
                    if (currentChar == '\r' && csvReader.ready()) {
                        csvReader.mark(1);
                        if (csvReader.read() != '\n') {
                            csvReader.reset();
                        }
                    }
                    columnValues.add(sb.toString());
                    break;
                } else {
                    sb.append(currentChar);
                }
            }

            if (intChar == -1) {
                if (sb.length() > 0 || !columnValues.isEmpty()) {
                    columnValues.add(sb.toString());
                }
            }

            isCurrentRowEmpty = true;
            for (int i = 0; i < columnValues.size(); i++) {
                String itemValue = columnValues.get(i);

                if (itemValue != null && !itemValue.isEmpty()) {
                    int startIndex = itemValue.startsWith("\"") ? 1 : 0;
                    int endIndex = itemValue.endsWith("\"") ? itemValue.length() - 1 : itemValue.length();

                    StringBuilder processedValue = new StringBuilder();
                    int quoteIndex = -1;
                    for (int j = startIndex; j < endIndex; j++) {
                        if (itemValue.charAt(j) == '\"') {
                            if (j - quoteIndex != 1) {
                                processedValue.append('\"');
                            }
                            quoteIndex = j;
                        } else {
                            processedValue.append(itemValue.charAt(j));
                        }
                    }
                    columnValues.set(i, processedValue.toString().trim());
                }

                if (!columnValues.get(i).isEmpty()) {
                    isCurrentRowEmpty = false;
                }
            }

            if (skipEmptyRows && isCurrentRowEmpty) {
                columnValues.clear();
            }

        } while (skipEmptyRows && isCurrentRowEmpty && csvReader.ready());

        return !columnValues.isEmpty();
    }

    public void writeFile(String[][] dataRows) {
        List<List<String>> originalData = new ArrayList<>();
        String filePath = getMasterDataFilePath();

        if (filePath != null && !filePath.isEmpty() && new File(filePath).exists()) {
            try (BufferedReader csvReader = new BufferedReader(new FileReader(filePath))) {
                csvReader.readLine();
                List<String> columnValues = new ArrayList<>();
                while (tryGetDataRow(csvReader, true, columnValues)) {
                    originalData.add(new ArrayList<>(columnValues));
                    columnValues.clear();
                }
            } catch (IOException e) {
                System.out.println("Error reading original CSV for comparison: " + e.getMessage());
            }
        }

        File originalFile = new File(filePath);
        String folderPath = originalFile.getParent();
        String outputFilePath = (folderPath == null) ? "output.csv" : folderPath + File.separator + "output.csv";
        String failedCasesFilePath = (folderPath == null) ? "failedCases.csv"
                : folderPath + File.separator + "failedCases.csv";

        int failCases = 0;

        try (
                BufferedWriter csvWriter = new BufferedWriter(new FileWriter(outputFilePath));
                BufferedWriter csvWriterFail = new BufferedWriter(new FileWriter(failedCasesFilePath))) {
            csvWriter.write("Message,Extracted,Expected");
            csvWriter.newLine();
            csvWriterFail.write("Message,Extracted,Expected");
            csvWriterFail.newLine();

            for (int i = 0; i < dataRows.length; i++) {
                boolean flag = false;
                String extractedOtp = dataRows[i][1];
                String expectedOtp = "";
                if (i < originalData.size() && originalData.get(i).size() > 1) {
                    expectedOtp = originalData.get(i).get(1);
                }

                if (dataRows[i].length != 2 || !extractedOtp.equalsIgnoreCase(expectedOtp)) {
                    failCases++;
                    flag = true;
                }

                List<String> formattedRow = new ArrayList<>();
                for (String data : dataRows[i]) {
                    StringBuilder sb = new StringBuilder();
                    boolean specialCharFound = false;
                    for (char currentChar : data.toCharArray()) {
                        sb.append(currentChar);
                        if (currentChar == '\"') {
                            sb.append(currentChar);
                        }
                        if (charsWithQuoteSet.contains(currentChar)) {
                            specialCharFound = true;
                        }
                    }
                    if (specialCharFound) {
                        sb.insert(0, '\"').append('\"');
                    }
                    formattedRow.add(sb.toString());
                }
                formattedRow.add(expectedOtp);

                String lineToWrite = String.join(",", formattedRow);
                csvWriter.write(lineToWrite);
                csvWriter.newLine();
                if (flag) {
                    csvWriterFail.write(lineToWrite);
                    csvWriterFail.newLine();
                }
            }

            double accuracy = dataRows.length > 0 ? ((double) (dataRows.length - failCases) / dataRows.length) * 100
                    : 0.0;
            System.out.printf("Accuracy: %.2f%%%n", accuracy);
            System.out.println("CSV writing completed successfully: " + outputFilePath);

        } catch (IOException ex) {
            System.out.println("Error writing CSV: " + ex.getMessage());
        }
    }

    public String extractOtp(String dataRow) {
        return "";
    }

    public String getMasterDataFilePath() {
        return "";
    }
}