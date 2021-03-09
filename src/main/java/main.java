import com.fasterxml.jackson.core.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class main {

    private String ssc; //Syntax: http(s)://<Hostname/IP>:<Port>
    public String token;
    public int currentVersionId;
    public int newVersionId;

    public void setURL(String sscURL) {
        ssc = sscURL;
    }

    //Use-Case 1 - Generate a Token
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
        System.out.println("------");
    }

    //Use-Case 2 - Uploading results
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

    //Use-Case 3 - Create a new version of an existing application
    public void createNewVersion(int currentVersionId) throws UnirestException, IOException {
        //Step 1 - Creating new version (not complete yet).
        this.currentVersionId = currentVersionId;
        System.out.println("Step 1 - Creating new version");

        HttpResponse<JsonNode> getCurrentVersion = Unirest.get(ssc + "/api/v1/projectVersions/66")
                .header("Authorization", "FortifyToken " + token)
                .header("accept", "application/json")
                .asJson();

        //create date with time for Token description
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);

        JSONObject bodyCreateNewVersion = new JSONObject();
        bodyCreateNewVersion.put("name", formattedDate);
        bodyCreateNewVersion.put("description", "");
        bodyCreateNewVersion.put("active", true);
        bodyCreateNewVersion.put("project", getCurrentVersion.getBody().getObject().getJSONObject("data").get("project"));
        bodyCreateNewVersion.put("issueTemplateId", "Prioritized-HighRisk-Project-Template");

        HttpResponse<JsonNode> createNewVersion = Unirest.post(ssc + "/api/v1/projectVersions")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(bodyCreateNewVersion)
                .asJson();

        System.out.println("Application created!");
        newVersionId = (int) createNewVersion.getBody().getObject().getJSONObject("data").get("id");
        System.out.println("New Version ID: " + newVersionId);
        System.out.println("Current Version ID: " +currentVersionId);
        System.out.println("------");

        //Step 2 - Copying attributes (Development Phase, Development Strategy, Accessibility, Application Type, etc.)
        System.out.println("Step 2 - Copying attributes");
        HttpResponse<JsonNode> getAttributesFromCurrentVersion = Unirest.get(ssc + "/api/v1/projectVersions/" + currentVersionId + "/attributes")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .asJson();

        //Paste Attributes in new Version
        HttpResponse<JsonNode> pasteAttributes = Unirest.put(ssc + "/api/v1/projectVersions/" + newVersionId + "/attributes")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(getAttributesFromCurrentVersion.getBody().getObject().get("data").toString())
                .asJson();

        System.out.println("Copying Attributes complete!");
        System.out.println("------");

        //Step 3 - Copying users
        System.out.println("Step 3 - Copying users");
        HttpResponse<JsonNode> getUsersFromCurrentVersion = Unirest.get(ssc + "/api/v1/projectVersions/" + currentVersionId + "/authEntities")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .asJson();

        HttpResponse<JsonNode> pasteUsers = Unirest.put(ssc + "/api/v1/projectVersions/" + newVersionId + "/authEntities")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(getUsersFromCurrentVersion.getBody().getObject().get("data").toString())
                .asJson();

        System.out.println("Copying Users complete!");
        System.out.println("------");

        //Step 4 - Copying responsibilities
        System.out.println("Step 4 - Copying responsibilities");
        HttpResponse<JsonNode> getResponsibilitiesFromCurrentVersion = Unirest.get(ssc + "/api/v1/projectVersions/" + currentVersionId + "/responsibilities")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .asJson();

        HttpResponse<JsonNode> pasteResponsibilities = Unirest.put(ssc + "/api/v1/projectVersions/" + newVersionId + "/responsibilities")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(getResponsibilitiesFromCurrentVersion.getBody().getObject().get("data").toString())
                .asJson();

        System.out.println("Copying Responsibilities complete!");
        System.out.println("------");

        //Step 5 - Copying other configuration
        System.out.println("Step 5 - Copying other configuration");

        //creating body
        JSONObject bodyOtherConf = new JSONObject();
        bodyOtherConf.put("copyAnalysisProcessingRules", true);
        bodyOtherConf.put("copyBugTrackerConfiguration", true);
        bodyOtherConf.put("copyCustomTags", true);
        bodyOtherConf.put("previousProjectVersionId", currentVersionId);
        bodyOtherConf.put("projectVersionId", newVersionId);

        HttpResponse<JsonNode> copyOtherConfiguration = Unirest.post(ssc + "/api/v1/projectVersions/action/copyFromPartial")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(bodyOtherConf)
                .asJson();

        System.out.println("Copying other configuration complete!");
        System.out.println("------");

        //Step 6 - Committing new version
        System.out.println("Step 6 - Committing new version");

        //creating body
        JSONObject bodyCommitNewVersion = new JSONObject();
        bodyCommitNewVersion.put("committed", true);

        HttpResponse<JsonNode> commitNewVersion = Unirest.put(ssc + "/api/v1/projectVersions/" + newVersionId)
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(bodyCommitNewVersion)
                .asJson();

        System.out.println("Committing complete!");
        System.out.println("------");

        //Step 7 - Copying scan results data (needs to take place after commit!)
        System.out.println("Step 7 - Copying scan results data");

        //create body
        JSONObject bodyCopyData = new JSONObject();
        bodyCopyData.put("projectVersionId", newVersionId);
        bodyCopyData.put("previousProjectVersionId", currentVersionId);

        HttpResponse<JsonNode> copyData = Unirest.post(ssc + "/api/v1/projectVersions/action/copyCurrentState")
                .header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(bodyCopyData)
                .asJson();

        System.out.println("Data copy complete!");

    }

    public static void main(String[] args) throws UnirestException, IOException, InterruptedException, ParseException {
        main test = new main();
        test.setURL("http://18.194.11.186:8080");
        test.createToken("UnifiedLoginToken");
        //test.uploadResult(3, "C:/dev/Benchmark-master/benchmark.fpr");
        test.createNewVersion(66);

    }
}
