package net.dinoculture.htr.web;

import java.util.Map;

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
public class UserApiController {
	private static final Logger logger = LoggerFactory.getLogger(UserApiController.class);
	@Autowired RedisService redisService;
	@Autowired ApiService apiService;
	
	@RequestMapping(value = "/public/api/updateInfo/{gameKey}/{token}", method = RequestMethod.POST)
	public Map update_info(@PathVariable("gameKey") String gameKey, @PathVariable("token") String token, @RequestBody Map<String, Object> params) throws Exception {
		logger.info("# /public/api/updateInfo/" + gameKey + "/" + token);
		
		if(gameKey == null) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_TOKEN, "gameKey not found.");
		}
		
		if(token == null) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_USER_KEY, "token not found.");
		}
		
		String userId = redisService.getUserId(gameKey, token);
		
		if(userId == null) {
			return DataUtil.getSuccessResult(Constants.CODE_RESULT_USER_KEY);
		}
		
		params.put("USER_ID", userId);
		
		if(params.containsKey("PASSWORD")) {																					// 패스워드 변경
			String password = params.get("PASSWORD").toString();
			password = StringUtil.makeSHA256(password);
			params.replace("PASSWORD", password);
		}
		
		Map resultLogin = apiService.executeBackend("user", "UserMapper", "updateUser", params);								// 데이터 수정
		
		if(!(boolean)resultLogin.get(Constants.REQUEST_RESULT_SUCCESS)){
			throw new Exception(resultLogin.get(Constants.REQUEST_RESULT_MESSAGE).toString());
		}
		
		redisService.replaceUserInfo(gameKey, token, params);																	// redis에서도 수정.
		
		return DataUtil.getSuccessResult(Constants.CODE_RESULT_SUCCESS);
	}
	
	@RequestMapping(value = "/public/api/updateData/{gameKey}/{token}", method = RequestMethod.POST)
	public Map update_data(@PathVariable("gameKey") String gameKey, @PathVariable("token") String token, @RequestBody String data) throws Exception {
		logger.info("# /public/api/updateData/" + gameKey + "/" + token);
		
		if(gameKey == null) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_ERROR_TOKEN, "gameKey not found.");
		}
		
		if(token == null) {
			return DataUtil.getFailResult(Constants.CODE_RESULT_USER_KEY, "token not found.");
		}
		
		String userId = redisService.getUserId(gameKey, token);
		
		if(userId == null) {
			return DataUtil.getSuccessResult(Constants.CODE_RESULT_USER_KEY);
		}
		
		
		redisService.updateUserData(gameKey, token, data);
		
		return DataUtil.getSuccessResult(Constants.CODE_RESULT_SUCCESS);
	}
}
