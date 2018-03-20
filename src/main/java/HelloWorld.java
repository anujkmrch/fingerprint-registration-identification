import spark.Request;
import spark.Response;
import spark.Route;

import spark.servlet.SparkApplication;
import spark.utils.IOUtils;

import static spark.Spark.*;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;


public class HelloWorld implements SparkApplication {
	static Logger log = Logger.getLogger(HelloWorld.class);

	public static void main(String[] args) {
//		staticFiles.location("/public");
		externalStaticFileLocation("/var/public");
		new HelloWorld().init();
	}

	@Override
	public void init() {
		boolean debug = true;
		String basePath = "/var/lib/tomcat7/uploader/rjs/";
		String uploadPathSave = basePath+"fingerprint/";
		String tempUploadSave = basePath+"fingerprint_tmp/"; //"/home/anuj/Desktop/pypack/tmp";
		String dataFile = basePath+"file.json";
		String matchUrl = "/rjs/fingerprint.upload.match";
		String setUrl = "/rjs/fingerprint.upload.set";
		boolean deleteTempFile = false;
		
		get("/",(req,res)->{
			return "<!DOCTYPE html>\n" + 
					"<html lang=\"en\">\n" + 
					"<head>\n" + 
					"    <meta charset=\"UTF-8\">\n" + 
					"    <title>Fingeprint web based fingerprint simulator</title>\n" + 
					"</head>\n" + 
					"<body>\n" + 
					"<h2>Match fingerprint to stored fingerprint database</h2>\n" + 
					"<hr>\n" + 
					"<div style=\"display:block\">\n" + 
					"    <form action=\""+matchUrl+"\" method=\"post\" enctype=\"multipart/form-data\">\n" + 
					"        <label for=\"myfile\">Select a file</label>\n" + 
					"        <input type=\"file\" id=\"myfile\" name=\"myfile\"/>\n" + 
					"        <input type=\"submit\" id=\"buttonUpload\" value=\"Upload\"/>\n" + 
					"    </form>\n" + 
					"</div><h2>Register Fingerprint with respect to the collegeid</h2>\n" + 
					"<hr>\n" + 
					"<div style=\"display:block\">\n" + 
					"    <form action=\""+setUrl+"\" method=\"post\" enctype=\"multipart/form-data\">\n" + 
					"        <label for=\"collegeId\">Enter college ID</label>\n" + 
					"        <input type=\"text\" id=\"collegeid\" name=\"collegeid\"/>\n" + 
					"        <label for=\"myfile\">Select a file</label>\n" + 
					"        <input type=\"file\" id=\"myfile\" name=\"myfile\" multiple/>\n" + 
					"        <input type=\"submit\" id=\"buttonUpload\" value=\"Upload\"/>\n" + 
					"    </form>\n" + 
					"</div>\n" + 
					"\n" + 
					"</body>\n" + 
					"</html>\n" + 
					"";
		});
		
		get("/hello", (req, res) -> "Hello World");
		
		post("/fingerprint.upload.match", (req, res) -> {
//			res.type("application/json");
			
			req.raw().setAttribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(tempUploadSave));
            Part filePart = req.raw().getPart("myfile");
            String collegeid = null;
            double threshold = 50;
            double high = 0;
            try {
            		
            if(filePart.getContentType().equalsIgnoreCase("image/jpeg") || filePart.getContentType().equalsIgnoreCase("image/png")) {
            	String ext = null;
            	
            	if(filePart.getContentType().equalsIgnoreCase("image/png")) {
            		ext = "png";
            	}

            	if(filePart.getContentType().equalsIgnoreCase("image/jpeg")) {
            		ext = "jpg";
            	}
	            try (InputStream inputStream = filePart.getInputStream()) {
	            	
	            	String filename = tempUploadSave + filePart.getName()+"."+ext;
	            	
	                OutputStream outputStream = new FileOutputStream(filename);
	                IOUtils.copy(inputStream, outputStream);
	                outputStream.close();
	                
	                byte[] image = Files.readAllBytes(Paths.get(filename));
	        		
	                FingerprintTemplate probe = new FingerprintTemplate()
	        			    .dpi(500)
	        			    .create(image);
	        		
	         		FingerprintMatcher matcher = new FingerprintMatcher()
			        .index(probe);
	
	        		JSONParser parser = new JSONParser();
	        		try {
	        			JSONArray data = (JSONArray) parser.parse(new FileReader(dataFile));
	            		for(int i=0;i<data.size();i++)
	            		{
	            			JSONObject jsonObject = (JSONObject) data.get(i);
//	            			System.out.println(jsonObject.get("collegeid"));
	            			FingerprintTemplate candidate = new FingerprintTemplate().deserialize(jsonObject.get("finger").toString());
	            			double score = matcher.match(candidate);
	            			if(score > high) {
	            				high = score;
	            				collegeid = jsonObject.get("collegeid").toString();
	            			}
//	            			System.out.println(score);
	            		}
	        		} catch(Exception e) {
	        			res.status(404);
	        			res.body(e.getMessage());
		                
//	        			log.info("Got an exception.", e);
//	        			e.printStackTrace();
	        		}

	        		//delete the uploaded fingerprint
	        		if(deleteTempFile) {
	        			File file = new File(filename);
	        			file.delete();
	        		}
	            } catch(Exception e) {
            		e.printStackTrace();
                	StringWriter sw = new StringWriter();
                	e.printStackTrace(new PrintWriter(sw));
                	String exceptionAsString = sw.toString();
                	return sw.toString();
//            		e.printStackTrace();            	
            	}

	            return high >= threshold ? "{success:true,collegeid:"+collegeid+",error:false,message:\"Found successsfully\"}" : "{success:false,collegeid:0,error:true,message:\"Not registered\"}";
            }
            
            } catch(Exception e) {
            	e.printStackTrace();
            	StringWriter sw = new StringWriter();
            	e.printStackTrace(new PrintWriter(sw));
            	String exceptionAsString = sw.toString();
            	return sw.toString();
            }
            
        	return "{success:false,error:true,message:\"fingerprint file type mismatch error, please contact developer\"}";            
        });
    	
    	post("/fingerprint.upload.set", (req, res) -> {
//    		res.type("application/json");
    		req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(uploadPathSave));
            Part filePart = req.raw().getPart("myfile");
            try {
            //check if the image file is png or jpg, if yes then proceed else through error
            if(filePart.getContentType().equalsIgnoreCase("image/jpeg") || filePart.getContentType().equalsIgnoreCase("image/png")) {
            	String ext = null;
            	//set extension type,png if png
            	if(filePart.getContentType().equalsIgnoreCase("image/png"))
            	{
            		ext = "png";
            	}
            	
            	//set extension type,jpg if jpg
            	if(filePart.getContentType().equalsIgnoreCase("image/jpeg"))
            	{
            		ext = "jpg";
            	}

            	String collegeid = req.queryParams("collegeid");

            	String filename = uploadPathSave + collegeid+"."+ext;

            	try (InputStream inputStream = filePart.getInputStream()) {
            	
            		OutputStream outputStream = new FileOutputStream(filename);
            		
//            		System.out.println(filePart.getContentType());
            		
            		IOUtils.copy(inputStream, outputStream);
            		
            		outputStream.close();
            	}
            	 catch(Exception e) {
//            		log.info("Got an exception.", e);
//             		e.printStackTrace();
            		res.status(500);
             		res.body(e.getMessage());
             	}
            	 
            	JSONParser parser = new JSONParser();
            	try {
            		JSONArray data = (JSONArray) parser.parse(new FileReader(dataFile));
            		double high = 0;

//            		System.out.println(filePart.getContentType());
            		
            		JSONObject obj = new JSONObject();
            		
            		byte[] image = Files.readAllBytes(Paths.get(filename));
            		
            		FingerprintTemplate template = new FingerprintTemplate()
        			    .dpi(500)
        			    .create(image);

            		obj.put("collegeid",collegeid);
            		obj.put("finger",template.serialize());
            		data.add(obj);

            		try (FileWriter file = new FileWriter(dataFile)) 
            		{
            			file.write(data.toJSONString());

            			// System.out.println("Successfully Copied JSON Object to File...");
            		}
            		
//            		System.out.println(data.size());
            	
            	} catch(Exception e) {
            		e.printStackTrace();
                	StringWriter sw = new StringWriter();
                	e.printStackTrace(new PrintWriter(sw));
                	String exceptionAsString = sw.toString();
                	return sw.toString();
//            		e.printStackTrace();            	
            	}
            	
            	return "{success:true,error:false,message:\"Fingerprint uploaded\"}";
            
            }
    	} catch(Exception e)
    	{
    		e.printStackTrace();
        	StringWriter sw = new StringWriter();
        	e.printStackTrace(new PrintWriter(sw));
        	String exceptionAsString = sw.toString();
        	return sw.toString();
    	}
            return "{success:false,error:true,message:\"fingerprint file type mismatch error, please contact developer\"}";                		
    	
    	});
    	
//		get("/rjs-center", (req, res) -> "Hello World RJS CENTER");
//		
//		get("/fingerprint.upload.match", (req, res) -> {
////    		byte[] image = Files.readAllBytes(Paths.get("/home/anuj/eclipse-workspace/my-app/src/main/resources/2_1.png"));
////    		FingerprintTemplate template = new FingerprintTemplate()
////    			    .dpi(500)
////    			    .create(image);
////
////    		byte[] image2 = Files.readAllBytes(Paths.get("/home/anuj/eclipse-workspace/my-app/src/main/resources/1_1.png"));
////
////    		FingerprintTemplate template2 = new FingerprintTemplate()
////    			    .dpi(500)
////    			    .create(image2);
////    		
////    		JSONArray main = new JSONArray();
////    		JSONArray fingers;
////    		JSONObject obj;
////    		
////    		obj = new JSONObject();
////    		obj.put("college_id","1234556");
////    		fingers = new JSONArray();    		
////
////    		obj.put("finger", template.serialize());
////    		main.add(obj);
////    		
////    		obj = new JSONObject();
////    		obj.put("college_id","1234557");
////    		
////    		fingers = new JSONArray();    		
////
////    		obj.put("finger", template2.serialize());
////    		main.add(obj);
////    		
////    		try (FileWriter file = new FileWriter("/home/anuj/Desktop/file.json")) {
////    			file.write(main.toJSONString());
////    			System.out.println("Successfully Copied JSON Object to File...");
////    		}
////    		FingerprintMatcher matcher = new FingerprintMatcher()
////    		        .index(template);
////    		
////    		JSONParser parser = new JSONParser();
////    		try {
////    			JSONArray data = (JSONArray) parser.parse(new FileReader("/home/anuj/Desktop/file.json"));
////    			double high = 0;
////        		for(int i=0;i<data.size();i++)
////        		{
////        			
////        			JSONObject jsonObject = (JSONObject) data.get(i);
////        			System.out.println(jsonObject.get("college_id"));
//////        			System.out.println(jsonObject.get("finger").toString());
////        			FingerprintTemplate template3 = new FingerprintTemplate().deserialize(jsonObject.get("finger").toString());
////        			double score = matcher.match(template3);
////        			System.out.println(score);
////        		}
////
////    		} catch(Exception e) {
////    			e.printStackTrace();
////    			
////    		}
//    		
//    		return "File uploaded and saved.";
//    	});
//		
//    	post("/fingerprint.upload.set", (req, res) -> {
//            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/home/anuj/Desktop/java/tmp/"));
//            Part filePart = req.raw().getPart("myfile");
//
//            try (InputStream inputStream = filePart.getInputStream()) {
//                OutputStream outputStream = new FileOutputStream("/home/anuj/Desktop/java/tmp/" + filePart.getSubmittedFileName());
//                IOUtils.copy(inputStream, outputStream);
//                outputStream.close();
//            }
//            
//            try {
//            	
//				byte[] probeImage = Files.readAllBytes(Paths.get("/home/anuj/Desktop/java/tmp/" + filePart.getSubmittedFileName()));
//				byte[] candidateImage = Files.readAllBytes(Paths.get("/home/anuj/eclipse-workspace/my-app/src/main/resources/2_1.png"));
//				FingerprintTemplate probe = new FingerprintTemplate()
//    				    .dpi(500)
//    				    .create(probeImage);
//    			FingerprintTemplate candidate = new FingerprintTemplate()
//    				    .dpi(500)
//    				    .create(candidateImage);
//    			double score = new FingerprintMatcher()
//    				    .index(probe)
//    				    .match(candidate);
//    			return score;
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			return Paths.get("probe.jpeg").toString();
//        });
    }
}