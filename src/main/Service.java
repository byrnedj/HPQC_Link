package main;

import java.io.Console;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.jsoup.Jsoup;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sun.misc.BASE64Encoder;




/**
 * This is it, the brains of the operation. In the Service class we do the following on the MAGIC workflow
 * 
 * 
 * 1. Authorize our HPQC client using LWSSO cookie
 * 2. GET JIRA issues
 * 3. If/when an issue is assigned to a release, create a new test case
 * 4. Monitor test case 
 * 5. If test case passed, advance the workflow of JIRA issue. don't worry about test case anymore
 * 6. If test case failed, look for the defect associated, update the JIRA issue with the info and assign to dev
 * 
 * Field Mappings for test case
 *     JIRA   |   HPQC
 *  --------------------- 
 *     Key    |   JIRA Key (user_01)
 *   Due Date |   Due Date
 *   Description | Description   
 *   Comments | Comments
 *   Summary | Test Name
 *   Issue category | Issue Category (user_02)
 *   Developer | append to comments
 *      BA     | append to comments
 * 
 * If you would like to add a project to this service do a "put" on the lProjects map in the main method. 
 * -The key is the JIRA project and the value is the HPQC project.
 * 
 * Important member fields of this class and their uses:
 * - HPQC_URI, the base URI (or URL for all matters), right now it is on the TEST_1 domain because that is where our JIRA_TEST is at
 * - JIRA_URI, the base URI (or URL for all matters), right now it is being hosted on our baby usnxv224, if you change JIRA servers, this needs changing.
 * 
 * 
 * A note about style,
 * -You may or may not like the Egyptian style brackets, spaces between parameter calls, or comments throughout. Deal with it because
 * that was how I learned to code.
 * -I try to keep consistent in variable naming convention: m prefix = member variable, l prefix = local variable, 
 *                                                          a prefix = argument variable
 * 
 * 
 * Dependencies,
 * -use maven
 * 
 * Debugging
 * -From the call to Console cons = System.console(); (line 123 atm) to the start of the scheduler are commented out
 *  because it won't currently work in eclipse
 * -To debug you should use the command line params and configure a debug configuration in Eclipse
 * -When doing a Maven Build/Install make sure to uncomment those.
 * 
 * TODO: There is a lot to do in the 2nd revision. Update to the current workflow requirements set by Toni Wilson.
 * TODO: Rewrite JIRA GET
 * TODO: Rewrite JIRA POST
 * TODO: Find a way to monitor
 * TODO: Rewrite HPQC GET - test cases are now something that we want
 * TODO: Rewrite HPQC POST - we need to create things now, check the HPQC ALM REST API guide.
 * 
 * @author Daniel Byrne - byrnedj12@gmail.com if he is not in the corporate directory.
 *
 */
public class Service 
{
	private static String HPQC_URI = "http://usnt248.na.intranet.msd:8080/qcbin/rest/domains/TEST_1/projects/";
	private static String JIRA_URI = "http://usnxv224:8080";
	private static String USER;
	private static String PWD;
	private static String DEFAULT_OWNER = "aiuaky5";

	/**
	 * @param args
	 */
	public static void main( String[] args ) throws ParseException
	{

		Options options = new Options();

		options.addOption( "user", true, "HPQC ID" );
		options.addOption( "password", true, "HPQC PWD" );

		CommandLineParser cliParser = new GnuParser();			

		//Parse the command line
		CommandLine cmd = cliParser.parse( options, args );


		USER = cmd.getOptionValue( "user" );
		PWD = cmd.getOptionValue( "password" );
		final Map<String, String> lProjects = new HashMap<String, String>();

//		Console cons = System.console();
//		USER = cons.readLine( "[%s]", "Username:" );
//
//		char[] lPasswd;
//		if ( ( lPasswd = cons.readPassword("[%s]", "Password:")) != null)
//		{
//			PWD = new String( lPasswd );
//		}
//
//		String done = "n";
//		while ( done.equalsIgnoreCase( "n" ) )
//		{
//			String JIRAproj = cons.readLine( "[%s]", "JIRA Project Key:" );
//			String HPQCproj = cons.readLine( "[%s]", "HPQC Project Name:" );
//			lProjects.put( JIRAproj, HPQCproj );
//			done = cons.readLine( "[%s]", "Done? (Y/N): " );
//		}

		//Subsequent cycles every 2 minutes
		ScheduledExecutorService scheduler =
				Executors.newSingleThreadScheduledExecutor();

		scheduler.scheduleAtFixedRate( new Runnable() 
		{ 
			public void run() 
			{ 
				cycle( lProjects ); 
			} 

		}, 0, 1, TimeUnit.MINUTES );

	}


	/**
	 * This is the main cycle.
	 */
	public static void cycle( Map<String,String> lProjects)
	{

		//KEY = JIRA project name
		//VALUE = HPQC project name
		//Map<String, String> lProjects = new HashMap<String, String>();
		//Projects go here:
		//lProjects.put( "AD", "JIRA_TEST" );

		//Loop through each project
		for ( Entry<String, String> project : lProjects.entrySet() )
		{

			//Get our project names from the map.
			String lJIRAProjectName = project.getKey();
			String lHPQCProjectName = project.getValue();

			System.out.println( "Begin cycle for JIRA project: " + lJIRAProjectName );

			//HPQC Init
			String LWSSO_COOKIE = authHPQC();
			Map<String,Issue> lHPQC = getHPQC( LWSSO_COOKIE, lHPQCProjectName );	

			//JIRA Init
			Map<String,Issue> lJIRA = getJIRA( lJIRAProjectName );

			//Resolve deltas (not the airline)

			//post up
			postHPQC( lHPQC, LWSSO_COOKIE, lHPQCProjectName );
			postJIRA ( lJIRA, lJIRAProjectName );

			System.out.println( "Ending cycle for JIRA project: " +  lJIRAProjectName );

		}
	}


	/**
	 * This is to get the LWSSO cookie from HPQC.
	 * @return the LWSSO cookie
	 */
	public static String authHPQC()
	{
		String cookie = "";
		try
		{
			URL authURL = new URL( "http://usnt248.na.intranet.msd:8080/qcbin/authentication-point/authenticate" );
			HttpURLConnection conn = (HttpURLConnection) authURL.openConnection();
			byte[] credBytes = ( USER + ":" + PWD).getBytes();
			String credEncodedString = "Basic " + new BASE64Encoder().encode( credBytes );
			conn.setRequestMethod( "POST" );
			conn.setRequestProperty( "Authorization", credEncodedString );
			cookie = conn.getHeaderFields().get( "Set-Cookie" ).get( 0 );
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}

		return cookie;

	}

	/**
	 * This gets the list of defects of the current HPQC project.
	 * @param LWSSO_COOKIE
	 * @return a map of the Issues (defects in HPQC), the key is the defect ID number
	 */
	public static Map<String, Issue> getHPQC( String LWSSO_COOKIE, String aProjectName )
	{
		Map<String, Issue> lDefectList = new HashMap<String, Issue>();
		try
		{
			URL authURL = new URL( HPQC_URI + aProjectName + "/defects" );
			HttpURLConnection conn = (HttpURLConnection) authURL.openConnection();

			conn.setRequestMethod( "GET" );
			conn.setRequestProperty( "Accept", "application/xml" );
			conn.setRequestProperty( "Cookie", LWSSO_COOKIE );

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document defects = docBuilder.parse( conn.getInputStream() );

		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return lDefectList;
	}

	/**
	 * Gets the list of issues based of a project name
	 * @param aProjectName the project that issues of HPQC Defect type will be retrieved
	 * @return a map of Issues, where key is equal to the HPQC Defect ID
	 * 
	 */
	public static Map<String, Issue> getJIRA( String aProjectName )
	{

		Map<String, Issue> lIssueList = new HashMap<String, Issue>();

		try
		{
			URL authURL = new URL( JIRA_URI + "/rest/api/2/search?jql=project=" + aProjectName + "&maxResults=10000" );
			HttpURLConnection conn = (HttpURLConnection) authURL.openConnection();
			byte[] credBytes = ( "sync" + ":" + "sync").getBytes();
			String credEncodedString = "Basic " + new BASE64Encoder().encode( credBytes );
			conn.setRequestProperty( "Authorization", credEncodedString );
			conn.setRequestMethod( "GET" );
			conn.setRequestProperty( "Accept", "application/json" );

			//Reader rabbit because even developers need to have fun.
			Reader rabbit = new InputStreamReader( conn.getInputStream() );
			Object issues = JSONValue.parse( rabbit );

		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return lIssueList;

	}

	
	/**
	 * Post up the new changes. 
	 * TODO: Rewrite with new specs
	 * @param aHPQC the issues to post
	 * @param LWSSO_COOKIE
	 */
	public static void postHPQC( Map<String, Issue> aHPQC, String LWSSO_COOKIE, String aProjectName )
	{
		for ( Entry<String, Issue> lEntry : aHPQC.entrySet() )
		{
			//issue to post
			Issue lIssue = lEntry.getValue();
			//only post if was changed.
			if ( lIssue.equals( "a" ) )
			{
				//This is done per the HPQC ALM REST API spec
				//First you need to lock the defect via a POST
				//Second you update the defect
				//Third you delete the lock on exit.
				try
				{
					URL lockURL = new URL( HPQC_URI + aProjectName + "/defects/" + lIssue.getKey() + "/lock/" );
					HttpURLConnection conn = (HttpURLConnection) lockURL.openConnection();

					conn.setRequestMethod( "POST" );
					conn.setRequestProperty( "Accept", "application/xml" );
					conn.setRequestProperty( "Cookie", LWSSO_COOKIE );

					URL putURL = new URL( HPQC_URI + aProjectName + "/defects/" + lIssue.getKey() );
					HttpURLConnection connPut = (HttpURLConnection) putURL.openConnection();

					connPut.setRequestMethod( "PUT" );
					connPut.setRequestProperty( "Accept", "application/xml" );
					connPut.setRequestProperty( "Content-Type", "application/xml" );
					connPut.setRequestProperty( "Cookie", LWSSO_COOKIE );
					connPut.setDoOutput( true );

					//Now we have to transform our DOM XML to a nice string representation to put on the sever
					Transformer transformer = TransformerFactory.newInstance().newTransformer();
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					//initialize StreamResult with File object to save to file
					StreamResult result = new StreamResult(new StringWriter());
					DOMSource source = new DOMSource(convertToXML( lIssue ));
					transformer.transform(source, result);
					//xmlString becomes what we write out
					String xmlString = result.getWriter().toString();

					OutputStreamWriter out = new OutputStreamWriter( connPut.getOutputStream() );
					out.write( xmlString );
					out.close();
					//always good to know results
					System.out.println( connPut.getResponseMessage() );

					//delete the lock
					conn.setRequestMethod( "DELETE" );
					conn.setRequestProperty( "Accept", "application/xml" );
					conn.setRequestProperty( "Cookie", LWSSO_COOKIE );

				}
				catch( Exception e )
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Post up to JIRA almighty
	 * TODO: Rewrite with new specs
	 * @param aJIRA the map of issues to post
	 * @param aProjectName the name of the project
	 */
	public static void postJIRA( Map<String, Issue> aJIRA, String aProjectName )
	{
		for ( Entry<String, Issue> lEntry : aJIRA.entrySet() )
		{
			Issue lIssue = lEntry.getValue();
			//if the entry was added we have to do a POST per the JIRA REST spec
			if ( lIssue.equals( "a" ))
			{
				JSONObject lToAdd = convertToJSON( lIssue, aProjectName, "create" );
				String issueCreateURL = JIRA_URI + "/rest/api/2/issue/";
				try
				{
					URL issueCreate = new URL( issueCreateURL );
					HttpURLConnection conn = (HttpURLConnection) issueCreate.openConnection();
					byte[] credBytes = ( "sync" + ":" + "sync").getBytes();
					String credEncodedString = "Basic " + new BASE64Encoder().encode( credBytes );
					conn.setDoOutput( true );
					conn.setRequestProperty( "Authorization", credEncodedString );
					conn.setRequestMethod( "POST" );
					conn.setRequestProperty( "Content-Type", "application/json" );
					OutputStreamWriter out = new OutputStreamWriter( conn.getOutputStream() );
					out.write( JSONObject.toJSONString( lToAdd ) );
					out.close();

					//Always nice to know it made it.
					System.out.println( "Create of HPQC ID: " + " was " + conn.getResponseMessage() );
					//System.out.println(JSONObject.toJSONString( lToAdd ));
					//Read response using reader rabbit
					Reader rabbit = new InputStreamReader( conn.getInputStream() );
					Map response = (Map)(JSONValue.parse( rabbit ));

					//Make sure to set the JIRA key once we made it.
					String lJIRAKey = response.get( "key" ).toString();
					aJIRA.put( lEntry.getKey(), lIssue );
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

			}
			//If the issue was simply updated we need to do a put per JIRA REST spec
			else if ( lIssue.equals( "" ) )
			{
				JSONObject lToAdd = convertToJSON( lIssue, aProjectName, "update" );
				//URI based off of issue key

				String issueChangedURL = JIRA_URI + "/rest/api/2/issue/" + lIssue.getKey();
				try
				{
					URL issueChanged = new URL( issueChangedURL );
					HttpURLConnection conn = (HttpURLConnection) issueChanged.openConnection();
					byte[] credBytes = ( "sync" + ":" + "sync").getBytes();
					String credEncodedString = "Basic " + new BASE64Encoder().encode( credBytes );
					conn.setDoOutput( true );
					conn.setRequestProperty( "Authorization", credEncodedString );
					conn.setRequestMethod( "PUT" );
					conn.setRequestProperty( "Content-Type", "application/json" );

					OutputStreamWriter out = new OutputStreamWriter( conn.getOutputStream() );
					out.write( JSONObject.toJSONString( lToAdd ) );
					out.close();

					System.out.println( "Update of HPQC ID: " + lIssue.getKey() + " was " + conn.getResponseMessage() );
					
					if ( conn.getResponseMessage().equals( "Bad Request" ) )
					{
						System.out.println(JSONObject.toJSONString( lToAdd ) );
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}

			}
		}
			
	}

	private static JSONObject convertToJSON( Issue aIssue, String aProjectName,
			String aString )
	{
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * Converts an issue to exporting in the XML format
	 * TODO: look at SAX parsing
	 * @param aIssue to convert to XML
	 * @return XML DOM representation
	 */
	public static Document convertToXML( Issue aIssue )
	{

		Document defect = null;
		try
		{

			//This XML file is built according to the spec in the HPQC ALM REST API 
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			defect = docBuilder.newDocument();

			//Start with <Entity Type="defect">
			Element root = defect.createElement( "Entity" );
			root.setAttribute( "Type", "defect" );
			defect.appendChild( root );

			//Setup fields
			Element fields = defect.createElement( "Fields" );
			root.appendChild( fields );

			//ID
			Element id = defect.createElement( "Field" );
			id.setAttribute( "Name", "id" );
			id.appendChild( defect.createElement( "Value" ) ).setTextContent( aIssue.getKey() );
			fields.appendChild( id );

			//synced fields
			Map<String, String> syncedFields = aIssue.getFields();
			for ( Entry<String, String> field : syncedFields.entrySet() )
			{
				Element lNewField = defect.createElement( "Field" );
				lNewField.setAttribute( "Name", field.getKey() );
				lNewField.appendChild( defect.createElement( "Value" ) ).setTextContent( field.getValue() );
				fields.appendChild( lNewField );
			}

		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		return defect;
	}

	

	
}
