package me.ym.kkp.sprinkle.service;

import lombok.extern.slf4j.Slf4j;

import me.ym.kkp.sprinkle.error.exception.ExpiredException;
import me.ym.kkp.sprinkle.error.exception.ForbiddenException;
import me.ym.kkp.sprinkle.error.exception.InvalidValueException;
import me.ym.kkp.sprinkle.model.Sprinkle;
import me.ym.kkp.sprinkle.repository.SprinkleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Transactional
@Service
public class SprinkleService {

    @Autowired
    SprinkleMapper sprinkleMapper;

    public Sprinkle selectMySprinkle(String token){
        return sprinkleMapper.selectMySprinkle(token);
    }

    public List<HashMap<String,Object>> selectMyReceiver(String token) {
        return sprinkleMapper.selectMyReceiver(token);
    }

    //랜덤금액 생성
    public int makeRandomReceivePrice(Sprinkle sprinkle) throws ParseException {
        int returnPrice=0;

        HashMap<String,Object> retmap = sprinkleMapper.selectSprinkleTotPrice(sprinkle);

        if(retmap == null){
            throw new InvalidValueException("!! Invalid token : " + sprinkle.getToken());      //잘못된 토큰 전송
        }

        //만료시간
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        Date validTime = formatter.parse(String.valueOf(retmap.get("validTime")));
        Calendar getValidTime = Calendar.getInstance();
        getValidTime.setTime(validTime);

        //현재시간
        Calendar curtime = Calendar.getInstance();
        curtime.setTime(new Date());

        long difference = (curtime.getTimeInMillis() - getValidTime.getTimeInMillis())/1000;

        if(difference > 0){
//            System.out.println("(curtime)부터 (getValidTime)까지 " + difference +"초가 지났습니다");
            // 1일 = 24 * 60 * 60
            throw new ExpiredException("!! Expired token : "+ (String) retmap.get("token")); //받기한 시간이 유효 토큰 만료시간 초과
        }

        if(retmap.get("sprinklerId").equals(sprinkle.getReceiverId())){
            throw new ForbiddenException("!! Unable access with userid : " + retmap.get("sprinklerId"));  //자신이 뿌린 건은 받기 불가
        }

        if(!"0".equals(String.valueOf(retmap.get("myIdCnt")))){
            throw new ForbiddenException("!! Only accessible once : " + sprinkle.getReceiverId());  //이미 받은 적이 있는 유저일 경우 받기 불가
        }

        String tempSprinklerPrice = String.valueOf(retmap.get("sprinklerPrice"));        //뿌린 금액
        String tempReceiverTotPrice = String.valueOf(retmap.get("receiverTotPrice"));   //실제 총 받은금액

        int sprinklerPrice = Integer.parseInt(tempSprinklerPrice);
        int receiverTotPrice = Integer.parseInt(tempReceiverTotPrice);
        int price = sprinklerPrice - receiverTotPrice;

        String tempReceiverCnt =   String.valueOf(retmap.get("receiverCnt"));           //받을수있는 총 인원
        String tempReceiverRealCnt =  String.valueOf(retmap.get("receiverRealCnt"));    //실제 받은 인원
        int receiverCnt = Integer.parseInt(tempReceiverCnt);
        int receiverRealCnt = Integer.parseInt(tempReceiverRealCnt);

        if(sprinklerPrice <= receiverTotPrice){
            throw new ForbiddenException("!! Unable access");      //받을 수 있는 금액과 인원이 이미 초과
        }else{
            if(receiverCnt == receiverRealCnt+1) {       //마지막으로 받기 가능한 자일 경우 남은 금액
                returnPrice = price;
            }else{
                returnPrice = (int) Math.floor(Math.random()*price+1); //1~price포함까지의 랜덤금액
            }
        }

        return returnPrice;
    }

    public int cancleSprinkle(Sprinkle sprinkle){
        return sprinkleMapper.cancleSprinkle(sprinkle);
    }
}
