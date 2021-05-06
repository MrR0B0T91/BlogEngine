package main.service;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import main.api.response.ErrorsResponse;
import main.api.response.RegisterResponse;
import main.model.CaptchaCodes;
import main.model.User;
import main.model.repositories.CaptchaCodeRepository;
import main.model.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {

  private final UserRepository userRepository;
  private final CaptchaCodeRepository captchaCodeRepository;

  @Autowired
  public RegisterService(UserRepository userRepository,
      CaptchaCodeRepository captchaCodeRepository) {
    this.userRepository = userRepository;
    this.captchaCodeRepository = captchaCodeRepository;
  }

  public RegisterResponse register(String email, String password, String name,
      String captcha, String captchaSecret) {

    ErrorsResponse errorsResponse = new ErrorsResponse();
    RegisterResponse registerResponse = new RegisterResponse();
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    CaptchaCodes repoCaptcha = captchaCodeRepository
        .findBySecret(captchaSecret);

    boolean dataCorrect = (checkCaptcha(captcha, repoCaptcha) && (checkEmail(email)));

    if (dataCorrect) {
      User user = new User();
      String encodedPassword = passwordEncoder.encode(password);

      user.setEmail(email);
      user.setName(name);
      user.setPassword(encodedPassword);
      user.setIsModerator(0);
      user.setRegTime(new Date());

      userRepository.save(user);
      registerResponse.setResult(true);

    } else {
      if (!checkEmail(email)) {
        errorsResponse.setEmail("Этот email уже зарегестрирован");
      }
      if (!checkCaptcha(captcha, repoCaptcha)) {
        errorsResponse.setCaptcha("Код с картинки введен неверно");
      }
      if (!checkName(name)) {
        errorsResponse.setName("Имя указано неверно");
      }
      registerResponse.setResult(false);
      registerResponse.setErrors(errorsResponse);
    }
    return registerResponse;
  }

  private boolean checkCaptcha(String captcha, CaptchaCodes repoCaptcha) {
    boolean check = true;
    boolean captchaCorrect = captcha.equals(repoCaptcha.getCode());
    if (!captchaCorrect) {
      check = false;
    }
    return check;
  }

  private boolean checkEmail(String email) {
    boolean check = true;
    User repoUser = userRepository.findByEmail(email);
    if (repoUser.getEmail().equals(email)) {
      check = false;
    }
    return check;
  }

  private boolean checkName(String name) {
    Pattern pattern = Pattern.compile("^(?=.{8,20}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$");
    Matcher matcher = pattern.matcher(name);
    boolean check = matcher.matches();

    return check;
  }
}
