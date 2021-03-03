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
    private static String token = "ZDBhYzk4MDEtYmU4ZC00Nzg0LTk0M2EtZjE4YzE4ZWUzZmNm";
    private static int projectVersion = 3;

    public static void testAPI() throws UnirestException {
        HttpResponse<JsonNode> sscApiTest = Unirest.get(ssc + "/api/v1/tokens")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                //ToDo
                //encrypt password
                .asJson();

        System.out.println(sscApiTest.getBody().toString());
    }
    public static void getLicense() throws UnirestException {
        HttpResponse<JsonNode> licenseInformation = Unirest.get(ssc + "/api/v1/license")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                //ToDo
                //encrypt password
                .asJson();

        System.out.println(licenseInformation.getBody().getObject().getJSONObject("data").getJSONArray("capabilities"));
    }

    public static void createApplication() throws UnirestException {

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
    public static void getProjectVersions() throws UnirestException {
        HttpResponse<JsonNode> getListOfProjectVersions = Unirest.get(ssc + "/api/v1/projectVersions")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .asJson();

        System.out.println(getListOfProjectVersions.getBody());
    }

    public static void createToken() throws UnirestException {
        //create date with time for Token description
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);

        //create body in JSON format
        JSONObject body = new JSONObject();
        body.put("description", formattedDate);
        body.put("type", "UnifiedLoginToken");

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
        String token = (String) createToken.getBody().getObject().getJSONObject("data").get("token");
        System.out.println(token);
    }

    public static void uploadResult() throws FileNotFoundException, UnirestException, InterruptedException {
        //upload results
        HttpResponse<JsonNode> uploadResult = Unirest.post(ssc + "/api/v1/projectVersions/" + projectVersion + "/artifacts")
                .header("Authorization", "FortifyToken " + token)
                .header("accept", "application/json")
                .field("file", new File("C:/dev/Benchmark-master/benchmark.fpr"))
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
                TimeUnit.SECONDS.sleep(5);
                continue;
            } else if(currentStatus.equals("REQUIRE_AUTH")){
                System.out.println("current status: " + currentStatus);
                System.out.println("Waiting for approval");

                TimeUnit.SECONDS.sleep(5);

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
                i++;
            } else {
                System.out.println("current status: " + currentStatus);
                i++;
            }
        }

    }

    public static void main(String[] args) throws UnirestException, FileNotFoundException, InterruptedException {
        //testAPI();
        //createApplication();
        //getProjectVersions();
        //createToken();
        uploadResult();

    }
}
