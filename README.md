# MDIDroid
Multi-Domain Isolation Android security system based on KitKat
***
MDIDroid is a security Android system based on Android 4.4.2 KitKat. The main purpose of the system is to protect different apps of different uses. As a customized system of [SEAndroid](https://source.android.com/security/selinux/index.html), we extend its MAC module to user-level. Instead of checking the signature to allocate the corresponding domain for the app in SEAndroid, MDIDroid check the package name for achieving more flexibility and freedom.    

We build serveral user domains to isolate different kinds of apps, like social ,shopping and work domain. A user must select one of the domains for his app during the installing process. Domain and domain are isolated, eg. file and intent. Cross-domain access is forbidden by our policies.   

We also build a test malware called Game. Game can access the root, read databases of other apps and intent another app. Game is to test the security functions of our system.    

[Demo Video](http://v.youku.com/v_show/id_XMTQ4MDkyNTg4OA==.html)   

Thanks to brilliant articles written by [luoshengyang](http://blog.csdn.net/luoshengyang). 

This project is a part of our work in National College Infomation Security Contest, here is the work document for the contest in Chinese. To learn more about SEAndroid, you can check [SEAndroid wiki](http://selinuxproject.org/page/NB_SEforAndroid_1).  


## How to use
MDIDroid is based on pure Android 4.4.2, so you can simply merge the /Android 4.4.2 folder to Android source code.It dose not guarantee that the code can be applied to other versions of Android, but you can migrate it to any versions of Android above ver. 4.4.  

We add a /XJTU tag to all the code we modified or added, I hope this will help you understand our system.  





> If you have any problem, please contact hevlhayt@foxmail.com (ﾉﾟ▽ﾟ)ﾉ

