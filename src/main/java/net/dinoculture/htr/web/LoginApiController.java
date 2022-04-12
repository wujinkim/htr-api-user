package net.dinoculture.htr.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.dinoculture.Constants;
import net.dinoculture.htr.service.RedisService;
import net.dinoculture.service.ApiService;
import net.dinoculture.util.DataUtil;
import net.dinoculture.util.StringUtil;

@RestController
public class LoginApiController {
	private static final Logger logger = LoggerFactory.getLogger(LoginApiController.class);
	@Autowired RedisService redisService;
	@Autowired ApiService apiService;
	
	@RequestMapping(value = "/public/api/login/{gameKey}", method = RequestMethod.POST)
	public Map login(@PathVariable("gameKey") String gameKey, @RequestBody Map<String, Object> params) throws Exception {
		logger.info("# /public/api/login/" + gameKey);
		
		if(gameKey == null) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_TOKEN, "gameKey not found.");
		}
		
		if(!params.containsKey("USER_KEY")) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_VALIDATION, "USER_KEY not found.");	
		}
		
		if(!params.containsKey("PASSWORD")) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_VALIDATION, "PASSWORD not found.");	
		}
		
		if(!params.containsKey("PLATFORM_TYPE")) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_VALIDATION, "PLATFORM_TYPE not found.");	
		}
		
		if(!params.containsKey("PLATFORM_VERSION")) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_VALIDATION, "PLATFORM_VERSION not found.");	
		}
		
		if(!params.containsKey("VERSION_ID")) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_VALIDATION, "VERSION_ID not found.");	
		}
		
		String userKey = params.get("USER_KEY").toString();
		String password = StringUtil.makeSHA256(params.get("PASSWORD").toString());
		Map userInfo = null;
		String token = redisService.getUserToken(gameKey, userKey);
		
		if(token != null) {																								// 캐시에 있다.
			userInfo = redisService.getUserInfo(gameKey, token);														// 캐시에서 조회
		}else {																											// 캐시에 없으면
			params.put("GAME_KEY", gameKey);
			Map resultInfo = apiService.executeBackend("user", "UserMapper", "viewUser", params);						// backend에서 조회
			if(!(boolean)resultInfo.get(Constants.REQUEST_RESULT_SUCCESS)){
				throw new Exception(resultInfo.get(Constants.REQUEST_RESULT_MESSAGE).toString());
			}
			if(resultInfo.get(Constants.REQUEST_RESULT_VIEW) == null) {													// backend에 없으면
				return DataUtil.getSuccessResult(Constants.CODE_RESULT_USER_KEY);										// 아이디 없음.
			}
			userInfo = (Map)resultInfo.get(Constants.REQUEST_RESULT_VIEW);
		}
		
		if(!userInfo.get("PASSWORD").toString().equals(password)) {														// 패스워드 불일치.
			return DataUtil.getSuccessResult(Constants.CODE_RESULT_USER_PASSWORD);
		}
		
		Map resultLogin = apiService.executeBackend("user", "UserMapper", "updateUserLogin", params);					// 로그인
		
		if(!(boolean)resultLogin.get(Constants.REQUEST_RESULT_SUCCESS)){
			throw new Exception(resultLogin.get(Constants.REQUEST_RESULT_MESSAGE).toString());
		}
		
		if(token == null) {
			token = redisService.putUserInfo(gameKey, userInfo);														// 사용자 정보 입력.
		}else {
			redisService.setUpdateTime(gameKey, token);																	// 최종 업데이트 시간 갱신
		}
		
		Map result = DataUtil.getSuccessResult(Constants.CODE_RESULT_SUCCESS);
		String etc = (String)userInfo.get("ETC");
		String data = (String)userInfo.get("USER_DATA");
		result.put("NICKNAME", userInfo.get("NICKNAME"));
		result.put("TOKEN", token);
		if(etc != null) {
			result.put("ETC", new ObjectMapper().readValue(etc, Map.class));
		}
		if(data != null) {
			result.put("USER_DATA", new ObjectMapper().readValue(data, Map.class));
		}
		
		return result;
	}
}
