import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import euphoria.*;
import euphoria.ConnectMessageEventListener;
import euphoria.WebsocketJSON.SendEvent;


public class euphoMail extends Bot{
  FileIO dataFile;
  //  !emailadd @TauNeutrin0 AKfycbyaxfE4lR-mVVs8ReJ4SQhchg5vg0Isbo8_XyswXeFPz5X2_-w
  //  !emailadd @Baconchicken42 AKfycbxW5Y8naA3qucnP6zKseTlmdU5VwTLioxKaXcWpWl3vZZC3raQ
  //  !emailadd @DoctorNumberFour AKfycbwwdEO-LL9zzszTaKVoU6q_od48WjINufej3ivc28UMJdAkO0c
  //  !emailadd @==> AKfycby3EtaE4Y6cBffAtt_dj1MWEoQ0lYJFg8TAtFRRercJpUj20VY
  //  !emailadd @Sumairu AKfycbzSDOTZv-XSJGgzZMafbNlJbite13_wiNs8Q8r2NYQPYkF2C1w
  //  !emailadd @JeremyRedFur AKfycbzqkLVHcj8lhbvdJ2MFWLFsRv-u-a9XDSRkj4KTGsbzB6gNOI_I
  JsonObject data;
  
  public euphoMail() {
    super("EuphoMail");
    initConsole();
    dataFile = new FileIO("euphoMail_data");
    data = dataFile.getJson();
    if(data.has("users")){
      if(!data.get("users").isJsonArray()){
        data.add("users", new JsonArray());
        try {
          dataFile.setJson(data);
        } catch (IOException e) {
          System.err.println("Could not setup users.");
        }
      }
    } else {
      data.add("users", new JsonArray());
      try {
        dataFile.setJson(data);
      } catch (IOException e) {
        System.err.println("Could not setup users.");
      }
    }
    addPacketEventListener(new ConnectMessageEventListener("euphoMail",this,dataFile).connectAll());
    
    if(!isConnected("bots")){
      connectRoom("bots");
    }
    addPacketEventListener(new MessageEventListener() {
      public void onSendEvent(MessageEvent evt) {
        //SendEvent data = (SendEvent)evt.getPacket().getData();
        if(evt.getMessage().matches("^!ping(?: @euphoMail)?$")){
          evt.reply("Pong!");
        }
        if(evt.getMessage().matches("^!help @euphoMail$")){
          String helpText = "Hello! This bot will send emails to users who have signed up.\n";
          helpText+="Usage \"!email @user Your message here!\"\n";
          helpText+="Commands:\n";
          helpText+="  !emailadd [@user] [id] - Adds a user (see instructions)\n";
          helpText+="  !emailchange [@user] [id] - Changes the app id for a user\n";
          helpText+="  !emailremove [@user] - Removes a user\n";
          helpText+="  !emaillist - Lists all current users\n";
          helpText+="If you want to be added to this bot, follow the instructions found here:\n";
          helpText+="https://drive.google.com/open?id=0B7uRDc-wgQQ0N3ZlaWNUTDFSek0\n";
          helpText+="If you want to add or remove this bot to/from another room, use \"!sendbot @euphoMail &room\" or \"!removebot @euphoMail &room\".\n";
          helpText+="This bot was made by @TauNeutrin0. It will not be online all the time - yet.";
          evt.reply(helpText);
        }
        if(evt.getMessage().matches("^!kill @euphoMail$")){
          evt.reply("/me is now exiting.");
          evt.getRoomConnection().closeConnection("Killed by room user.");
        }
        if(evt.getMessage().matches("^!pause @euphoMail$")){
          evt.reply("/me has been paused.");
          evt.getRoomConnection().pause("euphoMail");
        }
      }
      public void onSnapshotEvent(PacketEvent evt) {}
      public void onHelloEvent(PacketEvent evt) {
        evt.getRoomConnection().changeNick("euphoMail");
      }
      public void onNickEvent(PacketEvent evt) {}
      public void onJoinEvent(PacketEvent evt) {}
      public void onPartEvent(PacketEvent evt) {}
    });
    addPacketEventListener(new MessageEventListener(){
        @Override
        public void onSendEvent(MessageEvent evt) {
          String msg = evt.getMessage().replaceAll("[\u0000-\u001f]", "");
          if(msg.matches("^!email @[\\S]+ [\\s\\S]+$")){
            Pattern r = Pattern.compile("^!email @([\\S]+) ([\\s\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              for(int i=0;i<data.getAsJsonArray("users").size();i++) {
                String nick = data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString();
                String appId = data.getAsJsonArray("users").get(i).getAsJsonObject().get("id").getAsString();
                if(m.group(1).equals(nick)) {
                  evt.reply("Generating link...");
                  try {
                    String key = Integer.toString((int) System.currentTimeMillis()%(1000*60*60), 36);
                    key = key + new String(new char[5-key.length()]).replace("\0", "0");
                    System.out.println("Key generated: "+key);
                    if(sendPost(m.group(1), key, appId)) {
                      evt.reply("Click this link to send your email to @"+m.group(1)+": \nhttps://script.google.com/macros/s/"+appId+"/exec?message="+URLEncoder.encode(m.group(2)+"\n", "UTF-8")+"&sender="+URLEncoder.encode(evt.getSender(), "UTF-8")+"&key="+key);
                      System.out.println("Email link generated for @"+m.group(1)+" in &"+evt.getRoomConnection().getRoom()+".");
                    } else {
                      evt.reply("Failed to get link.");
                      System.out.println("Failed to set key.");
                    }
                  } catch (UnsupportedEncodingException e) {
                      e.printStackTrace();
                  } catch (IOException e) {
                    e.printStackTrace();
                  } catch (UnexpectedResponseException e) {
                    e.printStackTrace();
                  }
                }
              }
            }
          }
          
          
          else if(msg.matches("^!emailadd @[\\S]+ [\\S]+$")) {
            Pattern r = Pattern.compile("^!emailadd @([\\S]+) ([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isAdded = false;
              for(int i=0;i<data.getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isAdded = true;
                }
              }
              if(isAdded){
                evt.reply("@"+m.group(1)+" is already added. Try using \"!emailchange [@user] [id]\".\nTo check your stored id, use \"!emailcheck [@nick]\".");
              } else {
                JsonObject userObject = new JsonObject();
                userObject.addProperty("nick", m.group(1));
                userObject.addProperty("id", m.group(2));
                data.getAsJsonArray("users").add(userObject);
                try {
                  euphoMail.this.dataFile.setJson(data);
                  evt.reply("@"+m.group(1)+" has been added!");
                  System.out.println("@"+m.group(1)+" has been added with id "+m.group(2)+".");
                } catch (IOException e) {
                    e.printStackTrace();
                }
              }
              
            }
          }
          
          
          else if(msg.matches("^!emailremove @[\\S]+$")) {
            Pattern r = Pattern.compile("^!emailremove @([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isRemoved = false;
              for(int i=0;i<dataFile.getJson().getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isRemoved = true;
                  data.getAsJsonArray("users").remove(i);
                  try {
                    euphoMail.this.dataFile.setJson(data);
                    evt.reply("@"+m.group(1)+" has been removed.");
                    System.out.println("@"+m.group(1)+" has been removed.");
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
                }
              }
              if(!isRemoved){
                evt.reply("Could not find @"+m.group(1)+".");
              }
              
            }
          }
          
          
          else if(msg.matches("^!emailcheck @[\\S]+$")) {
            Pattern r = Pattern.compile("^!emailcheck @([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isAdded = false;
              for(int i=0;i<dataFile.getJson().getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isAdded = true;
                  evt.reply("@"+m.group(1)+" is added and has id: "+data.getAsJsonArray("users").get(i).getAsJsonObject().get("id").getAsString()+" .");
                }
              }
              if(!isAdded){
                evt.reply("Could not find @"+m.group(1)+".");
              }
              
            }
          }
          
          else if(msg.matches("^!emailchange @[\\S]+ [\\S]+$")) {
            Pattern r = Pattern.compile("^!emailchange @([\\S]+) ([\\S]+)$");
            Matcher m = r.matcher(msg);
            if (m.find()) {
              boolean isRemoved = false;
              for(int i=0;i<dataFile.getJson().getAsJsonArray("users").size();i++){
                if(data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString().equals(m.group(1))){
                  isRemoved = true;
                  JsonObject userObject = new JsonObject();
                  userObject.addProperty("nick", m.group(1));
                  userObject.addProperty("id", m.group(2));
                  data.getAsJsonArray("users").set(i, userObject);
                  try {
                    euphoMail.this.dataFile.setJson(data);
                    evt.reply("@"+m.group(1)+" has been updated.");
                    System.out.println("@"+m.group(1)+" has been updated with new id "+m.group(2)+".");
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
                }
              }
              if(!isRemoved){
                evt.reply("Could not find @"+m.group(1)+".");
              }
            }
          }
          
          else if(msg.matches("^!emaillist$")) {
            String text = "Current users:\n";
            for(int i=0;i<euphoMail.this.data.getAsJsonArray("users").size();i++) {
              text+="  "+euphoMail.this.data.getAsJsonArray("users").get(i).getAsJsonObject().get("nick").getAsString()+"\n";
            }
            evt.reply(text);
          }
        }
    });
  }
  
  private boolean sendPost(String sender, String key, String scriptID) throws UnsupportedEncodingException, IOException, UnexpectedResponseException{

    String urlStr = "https://script.google.com/macros/s/"+scriptID+"/exec?";
    URL url = new URL(urlStr);
    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

    //add reuqest header
    con.setRequestMethod("POST");

    // Encode url parameters
    String urlParameters = "sender="+URLEncoder.encode(sender,"UTF-8")+"&key="+URLEncoder.encode(key, "UTF-8");

    // Send post request
    con.setDoOutput(true);
    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
    wr.writeBytes(urlParameters);
    wr.flush();
    wr.close();

    int responseCode = con.getResponseCode();

    if (responseCode != 200) {
        throw new UnexpectedResponseException("Cheap Exception: Unexpected response code: "+responseCode);
    }

    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
    }
    in.close();

    return response.toString().equals("0");
  }
  
  
  class UnexpectedResponseException extends Exception {
    public UnexpectedResponseException(String err) {
      super(err);
    }
  }
  
  public static void main(String[] args) {
    new euphoMail();
  }
}
