package com.hayt.util;

import java.io.DataInputStream; 
import java.io.DataOutputStream; 
import java.io.IOException; 
 
import android.util.Log; 
 
public final class RootCmd { 
 
    private static final String TAG = "RootCmd"; 
    private static boolean mHaveRoot = false; 
 
    // �жϻ���Android�Ƿ��Ѿ�root�����Ƿ��ȡrootȨ�� 
    public static boolean haveRoot() { 
        if (!mHaveRoot) { 
            int ret = execRootCmdSilent("echo test"); // ͨ��ִ�в������������ 
            if (ret != -1) { 
                Log.i(TAG, "have root!"); 
                mHaveRoot = true; 
            } else { 
                Log.i(TAG, "not root!"); 
            } 
        } else { 
            Log.i(TAG, "mHaveRoot = true, have root!"); 
        } 
        return mHaveRoot; 
    } 
 
    // ִ������������� 
    public static String execRootCmd(String cmd) { 
        String result = ""; 
        DataOutputStream dos = null; 
        DataInputStream dis = null; 
         
        try { 
            Process p = Runtime.getRuntime().exec("su");// ����Root�����androidϵͳ����su���� 
            dos = new DataOutputStream(p.getOutputStream()); 
            dis = new DataInputStream(p.getInputStream()); 
 
            Log.i(TAG, cmd); 
            dos.writeBytes(cmd + "\n"); 
            dos.flush(); 
            dos.writeBytes("exit\n"); 
            dos.flush(); 
            String line = null; 
            while ((line = dis.readLine()) != null) { 
                Log.d("result", line); 
                result += line; 
            } 
            p.waitFor(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } finally { 
            if (dos != null) { 
                try { 
                    dos.close(); 
                } catch (IOException e) { 
                    e.printStackTrace(); 
                } 
            } 
            if (dis != null) { 
                try { 
                    dis.close(); 
                } catch (IOException e) { 
                    e.printStackTrace(); 
                } 
            } 
        } 
        return result; 
    } 
    // ִ���������ע������ 
    public static int execRootCmdSilent(String cmd) { 
        int result = -1; 
        DataOutputStream dos = null; 
         
        try { 
            Process p = Runtime.getRuntime().exec("su"); 
            dos = new DataOutputStream(p.getOutputStream()); 
             
            Log.i(TAG, cmd); 
            dos.writeBytes(cmd + "\n"); 
            dos.flush(); 
            dos.writeBytes("exit\n"); 
            dos.flush(); 
            p.waitFor(); 
            result = p.exitValue(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } finally { 
            if (dos != null) { 
                try { 
                    dos.close(); 
                } catch (IOException e) { 
                    e.printStackTrace(); 
                } 
            } 
        } 
        return result; 
    } 
} 