package net.dinoculture.htr.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections4.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import net.dinoculture.Constants;
import net.dinoculture.service.ApiService;
import net.dinoculture.util.StringUtil;

@Service
public class RedisService {
	public static final String REDIS_HEADER_USER_INFO = "USER_INFO:";
	public static final String REDIS_HEADER_USER_TOKEN = "USER_TOKEN:";
	public static final String REDIS_HEADER_USER_TIME = "USER_TIME:";
	
	private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
	@Autowired
	@Qualifier("redisTemplate") 
	private RedisTemplate redisTemplate;
	@Autowired
	private ApiService apiService;
	
	private HashOperations<String, String, String> redisUserValues = null;																	// 사용자 정보
	private ValueOperations<String, String> redisUserTokens = null;																			// 사용자 토큰
	private ValueOperations<String, String> redisUserTimes = null;																			// 사용자 업데이트 시간
	
	@Bean
	public void init() throws Exception {
		logger.info("======================================= RedisService.init start. =======================================");
		redisUserValues = redisTemplate.opsForHash();
		redisUserTokens = redisTemplate.opsForValue();
		redisUserTimes = redisTemplate.opsForValue();
	}
	
	/**
	 * 사용자 정보 입력
	 * @param gameKey
	 * @param userInfo
	 * @return
	 */
	public String putUserInfo(String gameKey, Map userInfo) {
		String token = UUID.randomUUID().toString().replace("-", "");
		String userKey = userInfo.get("USER_KEY").toString();
		String tokenKey = REDIS_HEADER_USER_TOKEN + gameKey + ":" + userKey;
		String InfoKey = REDIS_HEADER_USER_INFO + gameKey + ":" + token;
		String timeKey = REDIS_HEADER_USER_TIME + gameKey + ":" + token;
		redisUserValues.put(InfoKey, "USER_ID", userInfo.get("USER_ID").toString());
		redisUserValues.put(InfoKey, "USER_KEY", userKey);
		redisUserValues.put(InfoKey, "JOIN_TYPE", userInfo.get("JOIN_TYPE").toString());
		redisUserValues.put(InfoKey, "PASSWORD", userInfo.get("PASSWORD").toString());
		redisUserValues.put(InfoKey, "NICKNAME", userInfo.get("NICKNAME").toString());
		redisUserValues.put(InfoKey, "ENC_KEY", userInfo.get("ENC_KEY").toString());
		if(userInfo.containsKey("ETC") && userInfo.get("ETC") != null) {
			redisUserValues.put(InfoKey, "ETC", userInfo.get("ETC").toString());	
		}
		if(userInfo.containsKey("USER_DATA") && userInfo.get("USER_DATA") != null) {
			redisUserValues.put(InfoKey, "USER_DATA", userInfo.get("USER_DATA").toString());	
		}
		redisUserTokens.set(tokenKey, token);
		redisUserTimes.set(timeKey, String.valueOf(new Date().getTime()));
		return token;
	}
	
	/**
	 * 사용자 정보 수정
	 * @param gameKey
	 * @param token
	 * @param userInfo
	 */
	public void replaceUserInfo(String gameKey, String token, Map userInfo) {
		String InfoKey = REDIS_HEADER_USER_INFO + gameKey + ":" + token;
		if(userInfo.containsKey("PASSWORD")) {
			redisUserValues.put(InfoKey, "PASSWORD", userInfo.get("PASSWORD").toString());	
		}
		if(userInfo.containsKey("NICKNAME")) {
			redisUserValues.put(InfoKey, "NICKNAME", userInfo.get("NICKNAME").toString());	
		}
		if(userInfo.containsKey("ETC")) {
			redisUserValues.put(InfoKey, "ETC", userInfo.get("ETC").toString());	
		}
		setUpdateTime(gameKey, token);
	}
	
	/**
	 * 사용자 데이터 수정
	 * @param gameKey
	 * @param token
	 * @param data
	 */
	public void updateUserData(String gameKey, String token, String data) {
		String InfoKey = REDIS_HEADER_USER_INFO + gameKey + ":" + token;
		redisUserValues.put(InfoKey, "USER_DATA", data);
		setUpdateTime(gameKey, token);
	}
	
	/**
	 * 사용자 토큰 가져오기
	 * @param gameKey
	 * @param userKey
	 * @return
	 */
	public String getUserToken(String gameKey, String userKey) {
		String tokenKey = REDIS_HEADER_USER_TOKEN + gameKey + ":" + userKey;
		return redisUserTokens.get(tokenKey);
	}
	
	/**
	 * 사용자 정보 가져오기
	 * @param gameKey
	 * @param token
	 * @return
	 */
	public Map getUserInfo(String gameKey, String token) {
		String InfoKey = REDIS_HEADER_USER_INFO + gameKey + ":" + token;
		return redisUserValues.entries(InfoKey);
	}
	
	/**
	 * 사용자 아이디 가져오기
	 * @param gameKey
	 * @param token
	 * @return
	 */
	public String getUserId(String gameKey, String token) {
		String InfoKey = REDIS_HEADER_USER_INFO + gameKey + ":" + token;
		return redisUserValues.get(InfoKey, "USER_ID");
	}
	
	/**
	 * 최종 업데이트 시간 갱신
	 * @param gameKey
	 * @param token
	 */
	public void setUpdateTime(String gameKey, String token) {
		String timeKey = REDIS_HEADER_USER_TIME + gameKey + ":" + token;
		redisUserTimes.set(timeKey, String.valueOf(new Date().getTime()));
	}
}