package com.rsclouds.gtparallel.exception;

import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ServerException extends Exception {
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
	
	 
    private ServerException() {
        super();
    }
      
    private ServerException(String msg) {
        super(msg);
    }
    
    private ServerException(Throwable cause) {
        super(cause);
    }
    
    public ServerException(String errorCode,String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public ServerException(String errorCode,String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public Map<String,Object> toMap(){
    	Map<String,Object> response = new HashMap<String,Object>();
		response.put("errorCode", errorCode);
		response.put("errorMessage", errorMessage);
		response.put("statusCode", getStatusCode());
		return response;	
    }
    
    public String toJson(){
    	JSONObject response = new JSONObject();
    	response.put("errorCode", errorCode);
    	response.put("errorMessage", errorMessage);
		response.put("statusCode", getStatusCode()+"");
		return response.toString();	
    }
    
    public int getStatusCode(){
    	if(errorCode.equals("1001")){
    		return 500;
    	}else{
    		return 400;
    	}
    }


}
