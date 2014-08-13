package main;

import java.util.Date;
import java.util.Map;

/**
 * An entity is the top level of the hierarchy. 
 * 
 * 		Entity 
 * 		/	 \
 * 	 Issue  TestCase
 * 
 * These fields are the ones that have been defined as standard for all entities.
 * 
 * @author AIUAKY5
 *
 */
public interface Entity
{
	/**
	 * Get the status of the entity
	 * @return the status
	 */
	public abstract String getStatus();
	
	/**
	 * Set the status of the entity 
	 * @param aNewStatus
	 * @return true if successful, false otherwise
	 */
	public abstract boolean setStatus( String aNewStatus );
	
	/**
	 * Get the description of the entity
	 * @return the description
	 */
	public abstract String getDescription();
	
	/**
	 * Adds to the description field of the entity
	 * @param aNewDescription
	 * @return true if successful, false otherwise
	 */
	public abstract boolean addToDescription( String aNewDescription );
	
	/**
	 * Gets the due date of the entity
	 * @return the due date
	 */
	public abstract Date getDueDate();
	
	/**
	 * Sets a new due date of the entity
	 * @param aNewDueDate
	 * @return true if successful, false otherwise
	 */
	public abstract boolean setDueDate( Date aNewDueDate );
	
	/**
	 * Gets the comments of the entity
	 * @return the comments
	 */
	public abstract String getComments();
	
	/**
	 * Adds a new comment to the entity
	 * @param aNewComment
	 * @return true if successful, false otherwise
	 */
	public abstract boolean addComment( String aNewComment );
	
	/**
	 * Returns the fields of the issue, the key is the field name and the value is the field value
	 * @return map of the fields and their values
	 */
	public abstract Map<String,String> getFields();
	
	/**
	 * Sets new fields of the issue, the field name is the Entry key and value is the field value
	 * @param aNewFields
	 */
	public abstract void setFields( Map<String,String> aNewFields );
}
