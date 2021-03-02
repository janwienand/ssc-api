import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class main {

    public static void testAPI() throws UnirestException {
        HttpResponse<JsonNode> sscApiTest = Unirest.get("http://18.197.33.109:8080/api/v1/tokens")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .basicAuth("admin", "SSCpasswort123!")
                //ToDo
                //encrypt password
                .asJson();

        System.out.println(sscApiTest.getBody().toString());
    }

    public static void getLicense() throws UnirestException {
        HttpResponse<JsonNode> licenseInformation = Unirest.get("http://18.197.33.109:8080/api/v1/license")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("accept", "application/json")
                .basicAuth("admin", "SSCpasswort123!")
                //ToDo
                //encrypt password
                .asJson();

        System.out.println(licenseInformation.getBody().getObject().getJSONObject("data").getJSONArray("capabilities"));
    }

    public static void main(String[] args) throws UnirestException {
        System.out.println("Hello");
        testAPI();

    }
}
