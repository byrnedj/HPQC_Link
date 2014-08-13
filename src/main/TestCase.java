package main;

import java.util.Date;
import java.util.Map;

import org.w3c.dom.Document;

public class TestCase implements Entity
{
	private Document mXMLForm;

	public TestCase( Document aXMLForm )
	{
		mXMLForm = aXMLForm;
	}
	
	@Override
	public String getStatus()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setStatus( String aNewStatus )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getDescription()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addToDescription( String aNewDescription )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Date getDueDate()
	{
		// TODO Auto-generated method stub
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

	@Override
	public Map<String, String> getFields()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFields( Map<String, String> aNewFields )
	{
		// TODO Auto-generated method stub
		
	}

}
