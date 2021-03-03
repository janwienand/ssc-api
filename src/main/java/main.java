import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class main {

    private static String ssc = "http://35.156.197.254:8080";
    private static String token = "ZDBhYzk4MDEtYmU4ZC00Nzg0LTk0M2EtZjE4YzE4ZWUzZmNm";

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

        //get SSC password to create token
        auth sscAuth = new auth();

        //API request to create Token
        HttpResponse<JsonNode> createToken = Unirest.post(ssc + "/api/v1/tokens")
                .basicAuth(sscAuth.getUsername(), sscAuth.getPassword())
                //.header("Authorization", "FortifyToken " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .body(body)
                .asJson();

        //store Token in variable token
        String token = (String) createToken.getBody().getObject().getJSONObject("data").get("token");
        System.out.println(token);
    }

    public static void main(String[] args) throws UnirestException {
        //testAPI();
        //createApplication();
        //getProjectVersions();
        createToken();

    }
}
