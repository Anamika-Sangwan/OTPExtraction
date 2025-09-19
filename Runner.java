public class Runner {
    public static void main(String[] args) {

        Utility obj = new Child();

        String[] csvData = obj.readFileData();
        String[][] extractedData = new String[csvData.length][2];

        for (int i = 0; i < csvData.length; i++) {
            String otp = obj.extractOtp(csvData[i]);
            extractedData[i][0] = csvData[i];
            extractedData[i][1] = otp;
        }

        obj.writeFile(extractedData);
    }
}
