import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class main {

    private static String ssc = "http://35.156.197.254:8080";
    public static String token;
    public static String fileToken;

    public void createApplication() throws UnirestException {

        String body = "{  \"name\": \"1\",  \"description\": \"\",  \"active\": true,  \"committed\": false,  \"project\": {    \"name\": \"tokentest\",    \"description\": \"\",    \"issueTemplateId\": \"Prioritized-HighRisk-Project-Template\"  },  \"issueTemplateId\": \"Prioritized-HighRisk-Project-Template\"}";

        // {
        //   "name": "3",
        //   "description": "",
        //   "active": true,
        //   "committed": false,
        //   "project": {
        //     "name": "swagger",
        //     "description": "",
        //     "issueTemplateId": "Prioritized-HighRisk-Project-Template"
        //     },
        //   "issueTemplateId": "Prioritized-HighRisk-Project-Template"
        // }

        HttpResponse<JsonNode> createApplication = Unirest.post(ssc + "/api/v1/projectVersions")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(body)
                .asJson();

        System.out.println("Application created!");
        System.out.println(createApplication.getBody());
    }
    public void getProjectVersions() throws UnirestException {
        HttpResponse<JsonNode> getListOfProjectVersions = Unirest.get(ssc + "/api/v1/projectVersions")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .asJson();

        System.out.println(getListOfProjectVersions.getBody());
    }

    //Use-Case 1
    public void createToken(String tokenType) throws UnirestException {

        //create date with time for Token description
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);

        //create body in JSON format
        JSONObject body = new JSONObject();
        body.put("description", formattedDate);
        body.put("type", tokenType);

        //get SSC credentials to create token
        auth sscAuth = new auth();

        //API request to create Token
        HttpResponse<JsonNode> createToken = Unirest.post(ssc + "/api/v1/tokens")
                .basicAuth(sscAuth.getUsername(), sscAuth.getPassword())
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(body)
                .asJson();

        //store Token in variable token
        //String token = (String) createToken.getBody().getObject().getJSONObject("data").get("token");
        token = (String) createToken.getBody().getObject().getJSONObject("data").get("token");

        System.out.println(tokenType + " successfully created: " + token);
    }

    //Use-Case 2
    public void uploadResult(int projectVersion, String filePath) throws UnirestException, InterruptedException {
        //upload results
        HttpResponse<JsonNode> uploadResult = Unirest.post(ssc + "/api/v1/projectVersions/" + projectVersion + "/artifacts")
                .header("Authorization", "FortifyToken " + token)
                .header("accept", "application/json")
                .field("file", new File(filePath))
                .asJson();

        //store response id to artifactId
        int artifactId = (int) uploadResult.getBody().getObject().getJSONObject("data").get("id");

        System.out.println("Artifact id= " + artifactId);

        //creating a while loop to check the scan of the upload (if there is an approval needed)
        int i = 1;
        while (i < 2) {
            //check current status of uploaded file
            HttpResponse<JsonNode> checkIfApproved = Unirest.get(ssc + "/api/v1/artifacts/" + artifactId)
                    .header("Authorization", "FortifyToken " + token)
                    .header("accept", "application/json")
                    .asJson();

            //store current status
            String currentStatus = (String) checkIfApproved.getBody().getObject().getJSONObject("data").get("status");

            if (currentStatus.equals("SCHED_PROCESSING") || currentStatus.equals("PROCESSING")){
                System.out.println("current status: " + currentStatus + ". Wait another 5 seconds...");
                TimeUnit.SECONDS.sleep(5); //wait 5 seconds until next check
                continue;

            } else if(currentStatus.equals("REQUIRE_AUTH")){
                System.out.println("current status: " + currentStatus);
                System.out.println("Waiting for approval");

                //create list including artifactId
                List<Integer> list = new ArrayList<>();
                list.add(artifactId);

                //creating body for POST request
                JSONObject body = new JSONObject();
                body.put("artifactIds", new JSONArray(list));
                body.put("comment", "approved");

                //approve result upload
                HttpResponse<JsonNode> approveResult = Unirest.post(ssc + "/api/v1/artifacts/action/approve")
                        .header("Authorization", "FortifyToken " + token)
                        .header("accept", "application/json")
                        .header("Content-Type", "application/json")
                        .body(body)
                        .asJson();

                System.out.println("Successfully approved");
                i++; //end while loop
            } else {
                System.out.println("current status: " + currentStatus);
                i++; //end while loop
            }
        }

    }
    //Download result test
    public void downloadResult() throws UnirestException {
        JSONObject body = new JSONObject();
        body.put("fileTokenType", 1);

        HttpResponse<JsonNode> getFileToken = Unirest.post(ssc + "/api/v1/fileTokens")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json")
                .body(body)
                .asJson();
        fileToken = (String) getFileToken.getBody().getObject().getJSONObject("data").get("token");
        System.out.println("File Token: " + fileToken);

        HttpResponse<JsonNode> downloadResult = Unirest.get(ssc + "/download/artifactDownload.html?mat=" + fileToken + "&id=68")
                .header("Authorization", "FortifyToken " + token)
                .asJson();
    }


    public static void main(String[] args) throws UnirestException, FileNotFoundException, InterruptedException {
        main test = new main();
        test.createToken("UnifiedLoginToken");
        //test.uploadResult(3, "C:/dev/Benchmark-master/benchmark.fpr");

    }
}
