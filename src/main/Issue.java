package main;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

/**
 * The Issue class represents an issue that comes from JIRA
 * 
 * The fields that we care about are with reference to MAGIC are:
 * -Status 
 * -Key
 * -Due Date
 * -Description
 * -Comments
 * -Summary
 * -Issue Category
 * -Developer
 * -BA
 * 
 * Thanks for reading, you can get the author at:
 * @author Daniel Byrne - byrnedj12@gmail.com if he isn't in the corporate directory.
 *
 */
public class Issue implements Entity
{
	private JSONObject mIssue;
	private JSONObject mFields;
	private String mKey;
	
	
	public Issue( JSONObject aJSONForm )
	{
		mIssue = aJSONForm;
	}

	@Override
	public String getStatus()
	{
		JSONObject lFields = (JSONObject) mIssue.get( "fields" );
		String lStatus = "";
		if ( lFields.containsKey( "status" ) )
		{
			lStatus = (String) ( (JSONObject)lFields.get( "status" ) ).get( "name" );
		}
		return lStatus;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean setStatus( String aNewStatus )
	{
		JSONObject lFields = (JSONObject) mIssue.get( "fields" );
		String lStatus = "";
		if ( lFields.containsKey( "status" ) )
		{
			lStatus = (String) ( (JSONObject)lFields.get( "status" ) ).put( "name", aNewStatus );
		}
		
		if ( lStatus != null )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public String getDescription()
	{
		JSONObject lFields = (JSONObject) mIssue.get( "fields" );
		String lDescription = "";
		if ( lFields.containsKey( "description" ) )
		{
			lDescription = (String) lFields.get( "description" );
		}
		return lDescription;
	}

	@Override
	public boolean addToDescription( String aNewDescription )
	{
		JSONObject lFields = (JSONObject) mIssue.get( "fields" );
		String lDescription = "";
		if ( lFields.containsKey( "description" ) )
		{
			lDescription = this.getDescription() + " " + aNewDescription;
			lDescription = (String) lFields.put( "description", lDescription );

		}
		
		if ( lDescription != null )
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	@Override
	public Date getDueDate()
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
		JSONObject lFields = (JSONObject) mIssue.get( "fields" );

	
		String toParse = lFields.get( "key" ).toString();
		String left = toParse.substring( 0, toParse.indexOf( 'T' ) );
		String right = toParse.substring( toParse.indexOf( 'T' ) + 1, toParse.indexOf( '.' ) );
		toParse = left + " " + right;
		
		return null;
	}

	@Override
	public boolean setDueDate( Date aNewDueDate )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getComments()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addComment( String aNewComment )
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	/**
	 * Gets the key of the issue
	 * @return the key
	 */
	public String getKey()
	{
		return mKey;
	}
	
}
