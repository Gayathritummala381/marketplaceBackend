package com.designops.utility;

import java.util.Arrays;
import java.util.List;

public class Constant {

	
	public static String fromMail = "adidesignops@gmail.com";
	public static String host = "localhost";
	public static String password = "admin123";
	public static String randomPassword = "admin123";
	public static String defaultRoLeName="Subscriber";
	public static List<String> tenantColorList =
	Arrays.asList("#01b2ac", "#B02925","#679521", "#1A4A76", "#6C1A66", "#5A5714", "#553015", "#293838");
	public static List<String> tenantNameList=
	Arrays.asList("CS Digital", "RMPlus", "Wealth Engine", "Flowable", "DDH", "Score", "PAM", "Other");
	public static String elasticURL = "http://10.202.43.79:9200/";
	public static List<String> contentTypelist = Arrays.asList(".7z", ".bz2", ".bin", ".class", ".com", ".dat", ".deb",	".dll", ".dmg", ".ear",	".exe", ".gz", ".iso", ".jar", ".lz", ".mdb", ".mp3", ".mp4", ".msi", ".msp",".nupkg",".0", ".ocx", ".pkg", ".pyc", ".rar", ".rpm", ".rz" ,".so", ".tar", ".tgz", ".war", ".wav", ".wim", ".xz",".zip");
	public static String getTenantsForUserWithAdminRoleURL = "http://localhost:8080/tenantsByAdmin/";
	public static String getTenantByTenantIdURL = "http://localhost:8080/tenant/";
	public static String getALlArifactsByTenantIdURL = "http://localhost:8080/artifact/";
	public static String getUserByNameURL = "http://localhost:8080/users/";
	//public static String downloadURL = "http://gb1d9035018.eu.hedani.net:8080/marketplace-2.4.2/ downloadFolder FromIT/";
	public static String rootPath="/marketplace-2.4.2/index.html";
	public static String odysseyUserName = "admin";
	public static String odysseyPassword = "Admin@123";
	public static String bitbucketUser = "marketplace";
	public static String bitbucketPassword = "password";
	public static String artifactURL = "http://localhost:8046/";
	public static String repoName="artifactory/marketplace/";
	public static String bitbucketURL = "http://localhost:7990/scm/des/market.git";
	public static String uploadTempFolder = "C:\\Users\\GitFile\\upload_repo";
	public static String downloadTempFolder = "C:\\Users\\GitFile\\download_repo\\";
	public static String gitBranch = "RMPlus";
	public static String gitCommitMessage = "DESOPS-3 adding file";
	public static String downloadBitbucketURL = "https://bitbucket.org/nagini12/marketplace/raw/";

}
