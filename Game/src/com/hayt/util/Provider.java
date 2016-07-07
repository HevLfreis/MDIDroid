package com.hayt.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.NewsAddress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;

import android.content.pm.Signature;
import android.os.Environment;
import android.util.Xml;

public class Provider {
	
    final String mPackageName = "com.hayt.game";
    
    public String getPackageName() {
        return mPackageName;
    }

    private static final File[] INSTALL_POLICY_FILE = {
        new File(Environment.getDataDirectory(), "security/mac_permissions.xml"),
        new File(Environment.getRootDirectory(), "etc/security/mac_permissions.xml"),
        null};
    
    public boolean hasAccess(File[] policyFiles, String type, InputStream inputStream) {

//        FileReader policyFile = null;
//        int i = 0;
//        
//        
//        while (policyFile == null && policyFiles != null && policyFiles[i] != null) {
//            try {
//                policyFile = new FileReader(policyFiles[i]);
//                break;
//            } catch (FileNotFoundException e) {
//                //Slog.d(TAG,"Couldn't find install policy " + policyFiles[i].getPath());
//            }
//            i++;
//        }
//
//        if (policyFile == null) {
//            //Slog.d(TAG, "No policy file found. All seinfo values will be null.");
//            return false;
//        }

        //Slog.d(TAG, "Using install policy file " + policyFiles[i].getPath());


        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "UTF-8");
            //parser.setInput(policyFile);

            XmlUtils.beginDocument(parser, "policy");
            while (true) {
                XmlUtils.nextElement(parser);
                if (parser.getEventType() == XmlPullParser.END_DOCUMENT) {
                    break;
                }

                String tagName = parser.getName();
                if ("content".equals(tagName)) {
                    String pkgnam = parser.getAttributeValue(null, "pkgnam");
                    System.out.println(pkgnam);
                    if (pkgnam == null) {
                        //Slog.w(TAG, "<signer> without signature at "
                               //+ parser.getPositionDescription());
                        XmlUtils.skipCurrentTag(parser);
                        continue;
                    }
                    
                    if (!pkgnam.equals(getPackageName())) {
                    	XmlUtils.skipCurrentTag(parser);
                        continue;
					}
                    
                    String access = readAccessTag(parser, type);
                    
                   if (access.equals("true")) {
					return true;
				   }
                   else {
					return false;
                   }

                } else {
                    XmlUtils.skipCurrentTag(parser);
                    continue;
                }
            }
        } catch (XmlPullParserException e) {
            //Slog.w(TAG, "Got execption parsing ", e);
        } catch (IOException e) {
            //Slog.w(TAG, "Got execption parsing ", e);
        }
//        try {
//            policyFile.close();
//        } catch (IOException e) {
//            //omit
//        }
        return true;
    }
    
    private static String readAccessTag(XmlPullParser parser, String type) throws
    IOException, XmlPullParserException {

    	int t;
    	int outerDepth = parser.getDepth();
    	String access = null;
    	while ((t=parser.next()) != XmlPullParser.END_DOCUMENT
    			&& (t != XmlPullParser.END_TAG
           || parser.getDepth() > outerDepth)) {
    		if (t == XmlPullParser.END_TAG
    				|| t == XmlPullParser.TEXT) {
    			continue;
    		}

    		String tagName = parser.getName();
    		
    		//has problem
    		if ("access".equals(tagName)) {
    			String typeValue = parser.getAttributeValue(null, "type");
    			System.out.println(typeValue);
    			if (typeValue.equals(type)) {
					access = parser.getAttributeValue(null, "value");
					//System.out.println(access);
					break;
				}

    		}
    		XmlUtils.skipCurrentTag(parser);
    		}
    	
    	return access;
    	
    }
    
}
