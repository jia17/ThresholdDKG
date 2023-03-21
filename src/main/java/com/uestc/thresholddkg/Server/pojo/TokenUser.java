package com.uestc.thresholddkg.Server.pojo;

import com.uestc.thresholddkg.Server.user.TokenComm.GetTokenSi;
import com.uestc.thresholddkg.Server.util.DKG;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhangjia
 * @date 2023-02-11 16:28
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenUser {
    String user;
    String sign;
    String y;
    String hashToken;
    public TokenUser(String user,String sign,String y){
        this.user=user;this.sign=sign;this.y=y;
    }
    public void setHash(String s,String key){
        //System.out.println(s+";"+key);
        byte[] keyb=new byte[16];
        for(int i=0;i<keyb.length&&i<key.length();i++)keyb[i]=(byte) key.charAt(i);
        try {
            byte[] temp=DKG.HashSha256(s);
            this.hashToken=(DKG.HashBlake2bSalt(temp,keyb));
        }catch (Exception e) {e.printStackTrace();}
        //System.out.println(hashToken);
    }
    public boolean verifyHash(String s){
        byte[] keyb=new byte[16];
        String key= GetTokenSi.getHashTokenKey();
        for(int i=0;i<keyb.length&&i<key.length();i++)keyb[i]=(byte) key.charAt(i);
        byte[] temp=DKG.HashSha256(s);
        String hash=DKG.HashBlake2bSalt(temp,keyb);
        return hashToken.equals(hash);
    }
}
