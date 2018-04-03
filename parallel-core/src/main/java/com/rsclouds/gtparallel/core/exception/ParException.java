package com.rsclouds.gtparallel.core.exception;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ParException extends Exception {
	/**
     * 序列化ID
     */
    private static final long serialVersionUID = 1L;
	private String errorCode;
	private String errorMessage;
	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	 
    private ParException() {
        super();
    }
      
    private ParException(String msg) {
        super(msg);
    }
    
    private ParException(Throwable cause) {
        super(cause);
    }
    
    public ParException(String errorCode,String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public ParException(String errorCode,String errorMessage) {
        super();
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public Map<String,Object> toMap(){
    	Map<String,Object> response = new HashMap<String,Object>();
		response.put("errorCode", errorCode);
		response.put("errorMessage", errorMessage);
		return response;	
    }
    
    public String toJson() throws JSONException{
    	JSONObject response = new JSONObject();
    	response.put("errorCode", errorCode);
    	response.put("errorMessage", errorMessage);
		return response.toString();	
    }
    
    
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
