package main.service;

import java.util.List;
import main.api.response.CheckResponse;
import main.api.response.UserLoginResponse;
import main.model.ModerationStatus;
import main.model.Posts;
import main.model.repositories.PostRepository;
import main.model.repositories.UserRepository;
import main.springsecurity.UserDetailsServiceImpl;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class CheckService {

  private final UserDetailsServiceImpl userDetailsServiceImpl;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  private int moderationCount;

  CheckResponse checkResponse = new CheckResponse();
  UserLoginResponse userLoginResponse = new UserLoginResponse();

  public CheckService(
      UserDetailsServiceImpl userDetailsServiceImpl,
      PostRepository postRepository, UserRepository userRepository) {
    this.userDetailsServiceImpl = userDetailsServiceImpl;

    this.postRepository = postRepository;
    this.userRepository = userRepository;
  }

  public CheckResponse getResult() {

    List<Posts> newPosts = postRepository.findAllByModerationStatus(ModerationStatus.NEW);
    moderationCount = newPosts.size();

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof AnonymousAuthenticationToken)) {

      User user = (User) authentication.getPrincipal();
      main.model.User currentUser = userRepository.findByEmail(user.getUsername());

      userLoginResponse.setId(currentUser.getId());
      userLoginResponse.setName(currentUser.getName());
      userLoginResponse.setPhoto(currentUser.getPhoto());
      userLoginResponse.setEmail(currentUser.getEmail());
      userLoginResponse.setModeration(currentUser.getIsModerator() == 1);
      userLoginResponse.setSettings(true);

      if (userLoginResponse.isModeration()) {
        userLoginResponse.setModerationCount(moderationCount);
      } else {
        userLoginResponse.setModerationCount(0);
      }
      checkResponse.setResult(true);
      checkResponse.setUser(userLoginResponse);
    } else {
      checkResponse.setResult(false);
    }
    return checkResponse;
  }
}
