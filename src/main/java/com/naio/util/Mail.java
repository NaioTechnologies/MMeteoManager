package com.naio.util;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;

public class Mail
{
	private List<String> from;
	private List<String> to;
	private String subject;
	private String body;
	private Message message;
	private String attachedFilePath;
	private String attachedFileName;
	
	public Mail()
	{
		this.from = new ArrayList<String>();
		this.to = new ArrayList<String>();
	}

	public List<String> getFrom() {
		return from;
	}

	public void setFrom(List<String> from) {
		this.from = from;
	}

	public List<String> getTo() {
		return to;
	}

	public void setTo(List<String> to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public String getAttachedFilePath() {
		return attachedFilePath;
	}

	public void setAttachedFilePath(String attachedFilePath) {
		this.attachedFilePath = attachedFilePath;
	}

	public String getAttachedFileName() {
		return attachedFileName;
	}

	public void setAttachedFileName(String attachedFileName) {
		this.attachedFileName = attachedFileName;
	}
	
	
}
