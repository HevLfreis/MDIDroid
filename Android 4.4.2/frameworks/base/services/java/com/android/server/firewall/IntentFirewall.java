/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.firewall;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.XmlUtils;
import com.android.server.EventLogTags;
import com.android.server.IntentResolver;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class IntentFirewall {
    static final String TAG = "IntentFirewall";

    // e.g. /data/system/ifw or /data/secure/system/ifw
    private static final File RULES_DIR = new File(Environment.getSystemSecureDirectory(), "ifw");
    private static final File RULES_DIR_SEANDROID = new File("/data/security/current");
/*
    getDataDirectory=/data
    getDownloadCacheDirectory=/cache
    getExternalStorageAndroidDataDir=/mnt/sdcard/Android/data
    getExternalStorageDirectory=/mnt/sdcard 
    getExternalStorageState=removed
    getRootDirectory=/system 
    getSecureDataDirectory=/data 
    getSystemSecureDirectory=/data/system 
*/
    private static final int LOG_PACKAGES_MAX_LENGTH = 150;
    private static final int LOG_PACKAGES_SUFFICIENT_LENGTH = 125;

    private static final String TAG_RULES = "rules";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_SERVICE = "service";
    private static final String TAG_BROADCAST = "broadcast";

    private static final int TYPE_ACTIVITY = 0;
    private static final int TYPE_BROADCAST = 1;
    private static final int TYPE_SERVICE = 2;

    private static final HashMap<String, FilterFactory> factoryMap;

    private final AMSInterface mAms;

    private final RuleObserver mObserver;
    // private final RuleObserverSEAndroid mObserverSEAndroid;

    private FirewallIntentResolver mActivityResolver = new FirewallIntentResolver();
    private FirewallIntentResolver mBroadcastResolver = new FirewallIntentResolver();
    private FirewallIntentResolver mServiceResolver = new FirewallIntentResolver();

    private FirewallIntentResolverSEAndroid mDomainResolver =new FirewallIntentResolverSEAndroid();

    static {
        FilterFactory[] factories = new FilterFactory[] {
                AndFilter.FACTORY,
                OrFilter.FACTORY,
                NotFilter.FACTORY,

                StringFilter.ACTION,
                StringFilter.COMPONENT,
                StringFilter.COMPONENT_NAME,
                StringFilter.COMPONENT_PACKAGE,
                StringFilter.DATA,
                StringFilter.HOST,
                StringFilter.MIME_TYPE,
                StringFilter.SCHEME,
                StringFilter.PATH,
                StringFilter.SSP,

                CategoryFilter.FACTORY,
                SenderFilter.FACTORY,
                SenderPermissionFilter.FACTORY,
                PortFilter.FACTORY
        };

        // load factor ~= .75
        factoryMap = new HashMap<String, FilterFactory>(factories.length * 4 / 3);
        for (int i=0; i<factories.length; i++) {
            FilterFactory factory = factories[i];
            factoryMap.put(factory.getTagName(), factory);
        }
    }

    public IntentFirewall(AMSInterface ams) {
        mAms = ams;
        File rulesDir = getRulesDir();
        File rulesDirSEAndroid = getRulesDirSEAndroid();
        rulesDir.mkdirs();
        rulesDirSEAndroid.mkdirs();

        readRulesDir(rulesDir);
        Slog.w(TAG,"==IntentFirewall init call readRulesDirSEAndroid==");
        readRulesDirSEAndroid(rulesDirSEAndroid);

        mObserver = new RuleObserver(rulesDir);
        mObserver.startWatching();

        // mObserverSEAndroid = new RuleObserverSEAndroid(rulesDirSEAndroid);
        // mObserverSEAndroid.startWatching();
    }

    /**
     * This is called from ActivityManager to check if a start activity intent should be allowed.
     * It is assumed the caller is already holding the global ActivityManagerService lock.
     */
    public boolean checkStartActivity(Intent intent, int callerUid, int callerPid,
            String resolvedType, ApplicationInfo resolvedApp) {
        return checkIntent(mActivityResolver, intent.getComponent(), TYPE_ACTIVITY, intent,
                callerUid, callerPid, resolvedType, resolvedApp.uid);
    }

    public boolean checkService(ComponentName resolvedService, Intent intent, int callerUid,
            int callerPid, String resolvedType, ApplicationInfo resolvedApp) {
        return checkIntent(mServiceResolver, resolvedService, TYPE_SERVICE, intent, callerUid,
                callerPid, resolvedType, resolvedApp.uid);
    }

    public boolean checkBroadcast(Intent intent, int callerUid, int callerPid,
            String resolvedType, int receivingUid) {
        return checkIntent(mBroadcastResolver, intent.getComponent(), TYPE_BROADCAST, intent,
                callerUid, callerPid, resolvedType, receivingUid);
    }

    public boolean checkIntent(FirewallIntentResolver resolver, ComponentName resolvedComponent,
            int intentType, Intent intent, int callerUid, int callerPid, String resolvedType,
            int receivingUid) {
        boolean log = false;
        boolean block = false;

        // For the first pass, find all the rules that have at least one intent-filter or
        // component-filter that matches this intent
        List<Rule> candidateRules;
        candidateRules = resolver.queryIntent(intent, resolvedType, false, 0);
        if (candidateRules == null) {
            candidateRules = new ArrayList<Rule>();
        }
        resolver.queryByComponent(resolvedComponent, candidateRules);

        // For the second pass, try to match the potentially more specific conditions in each
        // rule against the intent
        for (int i=0; i<candidateRules.size(); i++) {
            Rule rule = candidateRules.get(i);
            if (rule.matches(this, resolvedComponent, intent, callerUid, callerPid, resolvedType,
                    receivingUid)) {
                block |= rule.getBlock();
                log |= rule.getLog();

                // if we've already determined that we should both block and log, there's no need
                // to continue trying rules
                if (block && log) {
                    break;
                }
            }
        }


        if(callerUid >10000 && receivingUid>10000){
            Slog.w(TAG,"\n==============callerUid:"+callerUid+",receivingUid:"+receivingUid+"=================");
            block = checkSEAndroidRule(callerUid,receivingUid);
        }


        if (log) {
            logIntent(intentType, intent, callerUid, resolvedType);
        }

        return !block;
    }




    public boolean checkSEAndroidRule(int callerUid,int receivingUid){

        boolean block = false;
            
        //find src package
        String src_package=null;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (pm != null) {
            try {
                String[] callerPackagesArray = pm.getPackagesForUid(callerUid);
                if (callerPackagesArray != null) {
                    src_package=callerPackagesArray[0];
                }
            } catch (RemoteException ex) {
                Slog.e(TAG, "Remote exception while retrieving packages:Caller package!!!", ex);
            }
        }
        //find dst package
        String dst_package=null;
        IPackageManager pm1 = AppGlobals.getPackageManager();
        if (pm1 != null) {
            try {
                String[] receiverPackagesArray = pm1.getPackagesForUid(receivingUid);
                if (receiverPackagesArray != null) {
            dst_package=receiverPackagesArray[0];
                }
            } catch (RemoteException ex) {
                Slog.e(TAG, "Remote exception while retrieving packages:receiver package!!!", ex);
            }
        }

        HashMap<String,Integer> level =new HashMap<String,Integer>();   //??????????????????????????????==>int and Integer ?????
        level.put("free_app",0);
        level.put("work_app",1);
        level.put("finance_app",1);
        level.put("shopping_app",2);
        level.put("social_app",2);
        level.put("normal_app",3);
        level.put("unknown_app",4);

        if(src_package==null || dst_package==null){
            // Slog.w(TAG, "CheckIntent:cannot find package");
        }
        else{       //both have package

            Slog.w(TAG, "==== src package: "+src_package+",dst package: "+dst_package+" ====");

			File rulesDirSEAndroid = getRulesDirSEAndroid();
            readRulesDirSEAndroid(rulesDirSEAndroid);

            // Slog.w(TAG,"-------- END READ SEA RULES!!!");

            // Slog.w(TAG, "package "+src_package+" & "+dst_package);
            String srcdomain = mDomainResolver.getDomain(src_package);
            String dstdomain = mDomainResolver.getDomain(dst_package);
            if(srcdomain==null || dstdoamin==null){ 
                //Slog.w(TAG,"CheckIntent:cannot find domain");
            }else{      //both have domain
                if(!srcdomain.equals(dstdomain)){
                    int srclevel = level.get(srcdomain);
                    int dstlevel = level.get(dstdomain);
                    if(srclevel > dstlevel) {  //block   
                        block = true;
                        Slog.w(TAG,"Permission Denied.Intent is blocked! Intent Info:");
                        Slog.w(TAG, "==== src package: "+src_package+" , dst package: "+dst_package+" ====");
                        Slog.w(TAG, "==== src domain: "+srcdomain+" , dst domain: "+dstdomain+" ====");
                    }
                }
            }
        }

        Slog.w(TAG,"------- block = "+block);

    return block;
    }

    private static void logIntent(int intentType, Intent intent, int callerUid,
            String resolvedType) {
        // The component shouldn't be null, but let's double check just to be safe
        ComponentName cn = intent.getComponent();
        String shortComponent = null;
        if (cn != null) {
            shortComponent = cn.flattenToShortString();
        }

        String callerPackages = null;
        int callerPackageCount = 0;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (pm != null) {
            try {
                String[] callerPackagesArray = pm.getPackagesForUid(callerUid);
                if (callerPackagesArray != null) {
                    callerPackageCount = callerPackagesArray.length;
                    callerPackages = joinPackages(callerPackagesArray);
                }
            } catch (RemoteException ex) {
                Slog.e(TAG, "Remote exception while retrieving packages", ex);
            }
        }

        EventLogTags.writeIfwIntentMatched(intentType, shortComponent, callerUid,
                callerPackageCount, callerPackages, intent.getAction(), resolvedType,
                intent.getDataString(), intent.getFlags());
    }

    /**
     * Joins a list of package names such that the resulting string is no more than
     * LOG_PACKAGES_MAX_LENGTH.
     *
     * Only full package names will be added to the result, unless every package is longer than the
     * limit, in which case one of the packages will be truncated and added. In this case, an
     * additional '-' character will be added to the end of the string, to denote the truncation.
     *
     * If it encounters a package that won't fit in the remaining space, it will continue on to the
     * next package, unless the total length of the built string so far is greater than
     * LOG_PACKAGES_SUFFICIENT_LENGTH, in which case it will stop and return what it has.
     */
    private static String joinPackages(String[] packages) {
        boolean first = true;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<packages.length; i++) {
            String pkg = packages[i];

            // + 1 length for the comma. This logic technically isn't correct for the first entry,
            // but it's not critical.
            if (sb.length() + pkg.length() + 1 < LOG_PACKAGES_MAX_LENGTH) {
                if (!first) {
                    sb.append(',');
                } else {
                    first = false;
                }
                sb.append(pkg);
            } else if (sb.length() >= LOG_PACKAGES_SUFFICIENT_LENGTH) {
                return sb.toString();
            }
        }
        if (sb.length() == 0 && packages.length > 0) {
            String pkg = packages[0];
            // truncating from the end - the last part of the package name is more likely to be
            // interesting/unique
            return pkg.substring(pkg.length() - LOG_PACKAGES_MAX_LENGTH + 1) + '-';
        }
        return null;
    }

    public static File getRulesDir() {
        return RULES_DIR;
    }

    public static File getRulesDirSEAndroid() {
        return RULES_DIR_SEANDROID;
    }

    /**
     * Reads rules from all xml files (*.xml) in the given directory, and replaces our set of rules
     * with the newly read rules.
     *
     * We only check for files ending in ".xml", to allow for temporary files that are atomically
     * renamed to .xml
     *d
     * All calls to this method from the file observer come through a handler and are inherently
     * serialized
     */
    private void readRulesDir(File rulesDir) {
        FirewallIntentResolver[] resolvers = new FirewallIntentResolver[3];
        for (int i=0; i<resolvers.length; i++) {
            resolvers[i] = new FirewallIntentResolver();
        }

        File[] files = rulesDir.listFiles();
        for (int i=0; i<files.length; i++) {
            File file = files[i];

            if (file.getName().endsWith(".xml")) {
                readRules(file, resolvers);
            }
        }

        Slog.i(TAG, "Read new rules (A:" + resolvers[TYPE_ACTIVITY].filterSet().size() +
                " B:" + resolvers[TYPE_BROADCAST].filterSet().size() +
                " S:" + resolvers[TYPE_SERVICE].filterSet().size() + ")");

        synchronized (mAms.getAMSLock()) {
            mActivityResolver = resolvers[TYPE_ACTIVITY];
            mBroadcastResolver = resolvers[TYPE_BROADCAST];
            mServiceResolver = resolvers[TYPE_SERVICE];
        }
    }

    private void readRulesDirSEAndroid(File rulesDir) {

        FirewallIntentResolverSEAndroid resolver = new FirewallIntentResolverSEAndroid();

        File[] files = rulesDir.listFiles();
        for (int i=0; i<files.length; i++) {
            File file = files[i];
            if (file.getName().equals("seapp_contexts")) {
                readRulesSEAndroid(file,resolver);
            }
        }

        synchronized (mAms.getAMSLock()) {
            mDomainResolver = resolver; 
            mDomainResolver.showDomains();
        }
    }

    private void readRulesSEAndroid(File rulesFile,FirewallIntentResolverSEAndroid resolver){
        String [] domains = {"unknown_app","normal_app" ,"finance_app" , "shopping_app","social_app","work_app","free_app"};
		FileReader fr = null;
        BufferedReader bufferedreader = null;
       try{
		   fr=new FileReader(rulesFile);
           bufferedreader = new BufferedReader(fr);  
                String instring = null; 
            while ((instring = bufferedreader.readLine().trim()) != null) {  
                if (0 != instring.length() && instring.charAt(0)!='#') { 
                    int pos = instring.indexOf("user=_app");
                    if(-1 != pos){
                        String[] part = instring.split(" ");
                        String[] packagepart = part[1].split("=");
                        String[] domainpart = part[2].split("=");
                        if(packagepart[0].equals("seinfo") && domainpart[0].equals("domain")){
                            String packages = packagepart[1].trim();
                            String domain = domainpart[1].trim();
                                                    for(String dm:domains){
                                                        if(domain.equals(dm)){
                                                                resolver.addPackage(domain,packages);  
                                                                break;
                                                        }
                                                    }
                                       }
                            }
                }
           }
           Slog.w(TAG,"end try");  
        } catch (IOException ex) {
            Slog.e(TAG, "Error reading SEAndroid rules from seap_contexts" , ex);
            return;
        }finally {
            if(bufferedreader != null){
             try {
                 fr.close();
                 bufferedreader.close();

                 Slog.w(TAG,"=================== SEAndroid File Closed! ====================");
                 return;        //???????????????????????????????????????????????????????????????????????????
             } catch (IOException ex) {
                 Slog.e(TAG, "Error while closing " + rulesFile, ex);
                 return;
             }
         }
         }
    }
  
    /**
     * Reads rules from the given file and add them to the given resolvers
     */
    private void readRules(File rulesFile, FirewallIntentResolver[] resolvers) {
        // some temporary lists to hold the rules while we parse the xml file, so that we can
        // add the rules all at once, after we know there weren't any major structural problems
        // with the xml file
        List<List<Rule>> rulesByType = new ArrayList<List<Rule>>(3);
        for (int i=0; i<3; i++) {
            rulesByType.add(new ArrayList<Rule>());
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(rulesFile);
        } catch (FileNotFoundException ex) {
            // Nope, no rules. Nothing else to do!
            return;
        }

        try {
            XmlPullParser parser = Xml.newPullParser();

            parser.setInput(fis, null);

            XmlUtils.beginDocument(parser, TAG_RULES);

            int outerDepth = parser.getDepth();
            while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                int ruleType = -1;

                String tagName = parser.getName();
                if (tagName.equals(TAG_ACTIVITY)) {
                    ruleType = TYPE_ACTIVITY;
                } else if (tagName.equals(TAG_BROADCAST)) {
                    ruleType = TYPE_BROADCAST;
                } else if (tagName.equals(TAG_SERVICE)) {
                    ruleType = TYPE_SERVICE;
                }

                if (ruleType != -1) {
                    Rule rule = new Rule();

                    List<Rule> rules = rulesByType.get(ruleType);

                    // if we get an error while parsing a particular rule, we'll just ignore
                    // that rule and continue on with the next rule
                    try {
                        rule.readFromXml(parser);
                    } catch (XmlPullParserException ex) {
                        Slog.e(TAG, "Error reading an intent firewall rule from " + rulesFile, ex);
                        continue;
                    }

                    rules.add(rule);
                }
            }

        } catch (XmlPullParserException ex) {
            // if there was an error outside of a specific rule, then there are probably
            // structural problems with the xml file, and we should completely ignore it
            Slog.e(TAG, "Error reading intent firewall rules from " + rulesFile, ex);
            return;
        } catch (IOException ex) {
            Slog.e(TAG, "Error reading intent firewall rules from " + rulesFile, ex);
            return;
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Slog.e(TAG, "Error while closing " + rulesFile, ex);
            }
        }

        for (int ruleType=0; ruleType<rulesByType.size(); ruleType++) {
            List<Rule> rules = rulesByType.get(ruleType);
            FirewallIntentResolver resolver = resolvers[ruleType];

            for (int ruleIndex=0; ruleIndex<rules.size(); ruleIndex++) {
                Rule rule = rules.get(ruleIndex);
                for (int i=0; i<rule.getIntentFilterCount(); i++) {
                    resolver.addFilter(rule.getIntentFilter(i));
                }
                for (int i=0; i<rule.getComponentFilterCount(); i++) {
                    resolver.addComponentFilter(rule.getComponentFilter(i), rule);
                }
            }
        }
    }

    static Filter parseFilter(XmlPullParser parser) throws IOException, XmlPullParserException {
        String elementName = parser.getName();

        FilterFactory factory = factoryMap.get(elementName);

        if (factory == null) {
            throw new XmlPullParserException("Unknown element in filter list: " + elementName);
        }
        return factory.newFilter(parser);
    }

    /**
     * Represents a single activity/service/broadcast rule within one of the xml files.
     *
     * Rules are matched against an incoming intent in two phases. The goal of the first phase
     * is to select a subset of rules that might match a given intent.
     *
     * For the first phase, we use a combination of intent filters (via an IntentResolver)
     * and component filters to select which rules to check. If a rule has multiple intent or
     * component filters, only a single filter must match for the rule to be passed on to the
     * second phase.
     *
     * In the second phase, we check the specific conditions in each rule against the values in the
     * intent. All top level conditions (but not filters) in the rule must match for the rule as a
     * whole to match.
     *
     * If the rule matches, then we block or log the intent, as specified by the rule. If multiple
     * rules match, we combine the block/log flags from any matching rule.
     */
    private static class Rule extends AndFilter {
        private static final String TAG_INTENT_FILTER = "intent-filter";
        private static final String TAG_COMPONENT_FILTER = "component-filter";
        private static final String ATTR_NAME = "name";

        private static final String ATTR_BLOCK = "block";
        private static final String ATTR_LOG = "log";

        private final ArrayList<FirewallIntentFilter> mIntentFilters =
                new ArrayList<FirewallIntentFilter>(1);
        private final ArrayList<ComponentName> mComponentFilters = new ArrayList<ComponentName>(0);
        private boolean block;
        private boolean log;

        @Override
        public Rule readFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
            block = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_BLOCK));
            log = Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_LOG));

            super.readFromXml(parser);
            return this;
        }

        @Override
        protected void readChild(XmlPullParser parser) throws IOException, XmlPullParserException {
            String currentTag = parser.getName();

            if (currentTag.equals(TAG_INTENT_FILTER)) {
                FirewallIntentFilter intentFilter = new FirewallIntentFilter(this);
                intentFilter.readFromXml(parser);
                mIntentFilters.add(intentFilter);
            } else if (currentTag.equals(TAG_COMPONENT_FILTER)) {
                String componentStr = parser.getAttributeValue(null, ATTR_NAME);
                if (componentStr == null) {
                    throw new XmlPullParserException("Component name must be specified.",
                            parser, null);
                }

                ComponentName componentName = ComponentName.unflattenFromString(componentStr);
                if (componentName == null) {
                    throw new XmlPullParserException("Invalid component name: " + componentStr);
                }

                mComponentFilters.add(componentName);
            } else {
                super.readChild(parser);
            }
        }

        public int getIntentFilterCount() {
            return mIntentFilters.size();
        }

        public FirewallIntentFilter getIntentFilter(int index) {
            return mIntentFilters.get(index);
        }

        public int getComponentFilterCount() {
            return mComponentFilters.size();
        }

        public ComponentName getComponentFilter(int index) {
            return mComponentFilters.get(index);
        }
        public boolean getBlock() {
            return block;
        }

        public boolean getLog() {
            return log;
        }
    }

    private static class FirewallIntentFilter extends IntentFilter {
        private final Rule rule;

        public FirewallIntentFilter(Rule rule) {
            this.rule = rule;
        }
    }

    private static class FirewallIntentResolver
            extends IntentResolver<FirewallIntentFilter, Rule> {
        @Override
        protected boolean allowFilterResult(FirewallIntentFilter filter, List<Rule> dest) {
            return !dest.contains(filter.rule);
        }

        @Override
        protected boolean isPackageForFilter(String packageName, FirewallIntentFilter filter) {
            return true;
        }

        @Override
        protected FirewallIntentFilter[] newArray(int size) {
            return new FirewallIntentFilter[size];
        }

        @Override
        protected Rule newResult(FirewallIntentFilter filter, int match, int userId) {
            return filter.rule;
        }

        @Override
        protected void sortResults(List<Rule> results) {
            // there's no need to sort the results
            return;
        }

        public void queryByComponent(ComponentName componentName, List<Rule> candidateRules) {
            Rule[] rules = mRulesByComponent.get(componentName);
            if (rules != null) {
                candidateRules.addAll(Arrays.asList(rules));
            }
        }

        public void addComponentFilter(ComponentName componentName, Rule rule) {
            Rule[] rules = mRulesByComponent.get(componentName);
            rules = ArrayUtils.appendElement(Rule.class, rules, rule);
            mRulesByComponent.put(componentName, rules);
        }

        private final ArrayMap<ComponentName, Rule[]> mRulesByComponent =
                new ArrayMap<ComponentName, Rule[]>(0);
    }

     private static class FirewallIntentResolverSEAndroid{
        
        private HashMap<String,ArrayList<String>> mDomainPackages = new HashMap<String,ArrayList<String>>();
        String [] domains = {"unknown_app","normal_app" ,"finance_app" , "shopping_app","social_app","work_app","free_app"};

        public FirewallIntentResolverSEAndroid(){
        }

        public void addPackage(String tmpDoamin,String tmpPackage){
            ArrayList<String> dm =  mDomainPackages.get(tmpDoamin);
            if(dm != null){
                for(String pkg:dm){
                    if(pkg.equals(tmpPackage)){
                        break;
                    }
                }
                mDomainPackages.get(tmpDoamin).add(tmpPackage);
            }else{
                ArrayList<String> mpkg = new ArrayList<String>();
                mpkg.add(tmpPackage);
                mDomainPackages.put(tmpDoamin,mpkg);
                // Slog.w(TAG,"add package "+tmpPackage +" to domain "+tmpDoamin);
            }
        }

        public HashMap<String,ArrayList<String>> getDomainPackages(){
            return mDomainPackages;
        }

        public String getDomain(String pkg){
                for(String dm:domains){
                     ArrayList<String> tmp = mDomainPackages.get(dm);
                     if(tmp != null){
                        for(String packages:tmp){
                            if(packages.equals(pkg))
                                return dm;
                        }
                     }
                }
                return null;
        }

        public void showDomains(){
            Slog.w(TAG,"=====================BEGIN TO SHOW DOMAINS=========================");
            for(String dm:domains){
                ArrayList<String> tmp = mDomainPackages.get(dm);
                if(tmp != null)
                    for(String packages:tmp){
                        Slog.w(TAG,dm+":"+packages);
                    }
            }
            Slog.w(TAG,"=======================END SHOW DOMAINS============================");
        }
     }

    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            readRulesDir(getRulesDir());
        }
    };

/*    final Handler mHandlerAEAndroid = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Slog.w(TAG,"===mHandlerAEAndroid call readRulesDirSEAndroid==");
            readRulesDirSEAndroid(getRulesDirSEAndroid());
        }
    };*/


    /**
     * Monitors for the creation/deletion/modification of any .xml files in the rule directory
     */
    private class RuleObserver extends FileObserver {
        private static final int MONITORED_EVENTS = FileObserver.CREATE|FileObserver.MOVED_TO|
                FileObserver.CLOSE_WRITE|FileObserver.DELETE|FileObserver.MOVED_FROM;

        public RuleObserver(File monitoredDir) {
            super(monitoredDir.getAbsolutePath(), MONITORED_EVENTS);
        }

        @Override
        public void onEvent(int event, String path) {
            if (path.endsWith(".xml")) {
                // we wait 250ms before taking any action on an event, in order to dedup multiple
                // events. E.g. a delete event followed by a create event followed by a subsequent
                // write+close event
                mHandler.removeMessages(0);
                mHandler.sendEmptyMessageDelayed(0, 250);
            }
        }
    }


/*    private class RuleObserverSEAndroid extends FileObserver {
        private static final int MONITORED_EVENTS = FileObserver.CREATE|FileObserver.MOVED_TO|
                FileObserver.CLOSE_WRITE|FileObserver.DELETE|FileObserver.MOVED_FROM;

        public RuleObserverSEAndroid(File monitoredDir) {
            super(monitoredDir.getAbsolutePath(), MONITORED_EVENTS);
        }

        @Override
        public void onEvent(int event, String path) {
            //if (path.endsWith(".xml")) {
                // we wait 250ms before taking any action on an event, in order to dedup multiple
                // events. E.g. a delete event followed by a create event followed by a subsequent
                // write+close event
                mHandlerAEAndroid.removeMessages(0);
                mHandlerAEAndroid.sendEmptyMessageDelayed(0, 250);
            //}
        }
    }*/



    /**
     * This interface contains the methods we need from ActivityManagerService. This allows AMS to
     * export these methods to us without making them public, and also makes it easier to test this
     * component.
     */
    public interface AMSInterface {
        int checkComponentPermission(String permission, int pid, int uid,
                int owningUid, boolean exported);
        Object getAMSLock();
    }

    /**
     * Checks if the caller has access to a component
     *
     * @param permission If present, the caller must have this permission
     * @param pid The pid of the caller
     * @param uid The uid of the caller
     * @param owningUid The uid of the application that owns the component
     * @param exported Whether the component is exported
     * @return True if the caller can access the described component
     */
    boolean checkComponentPermission(String permission, int pid, int uid, int owningUid,
            boolean exported) {
        return mAms.checkComponentPermission(permission, pid, uid, owningUid, exported) ==
                PackageManager.PERMISSION_GRANTED;
    }

    boolean signaturesMatch(int uid1, int uid2) {
        try {
            IPackageManager pm = AppGlobals.getPackageManager();
            return pm.checkUidSignatures(uid1, uid2) == PackageManager.SIGNATURE_MATCH;
        } catch (RemoteException ex) {
            Slog.e(TAG, "Remote exception while checking signatures", ex);
            return false;
        }
    }

}
