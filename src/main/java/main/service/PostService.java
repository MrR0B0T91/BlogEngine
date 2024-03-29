package main.service;

import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import main.api.requset.CommentRequest;
import main.api.requset.LikeDislikeRequest;
import main.api.requset.ModerateRequest;
import main.api.requset.PostRequest;
import main.api.response.CalendarResponse;
import main.api.response.CommentResponse;
import main.api.response.GeneralResponse;
import main.api.response.PostByIdResponse;
import main.api.response.PostResponse;
import main.api.response.PostingResponse;
import main.dto.CommentDto;
import main.dto.CommentUserDto;
import main.dto.PostDto;
import main.dto.PostErrorDto;
import main.dto.UserDto;
import main.model.GlobalSettings;
import main.model.PostComments;
import main.model.PostVotes;
import main.model.Posts;
import main.model.Tag2Post;
import main.model.Tags;
import main.model.enums.ModerationStatus;
import main.model.repositories.GlobalSettingsRepository;
import main.model.repositories.PostCommentRepository;
import main.model.repositories.PostRepository;
import main.model.repositories.PostVoteRepository;
import main.model.repositories.Tag2PostRepository;
import main.model.repositories.TagRepository;
import main.model.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostCommentRepository postCommentRepository;
    private final PostVoteRepository postVoteRepository;
    private final TagRepository tagRepository;
    private final Tag2PostRepository tag2PostRepository;
    private final GlobalSettingsRepository globalSettingsRepository;

    private Sort sort;
    private final int MAX_LENGTH = 150;
    private final int MIN_TITLE_LENGTH = 3;
    private final int MIN_TEXT_LENGTH = 10;
    public static final int moderatorId = 5;

    public PostService(PostRepository postRepository,
                       UserRepository userRepository,
                       PostCommentRepository postCommentRepository,
                       PostVoteRepository postVoteRepository, TagRepository tagRepository,
                       Tag2PostRepository tag2PostRepository,
                       GlobalSettingsRepository globalSettingsRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postCommentRepository = postCommentRepository;
        this.postVoteRepository = postVoteRepository;
        this.tagRepository = tagRepository;
        this.tag2PostRepository = tag2PostRepository;
        this.globalSettingsRepository = globalSettingsRepository;
    }


    public PostResponse getPosts(int offset, int limit, String mode) {

        PostResponse postResponse = new PostResponse();

        if (mode.equals("recent")) {
            sort = Sort.by("time").descending();
            Page<Posts> recentPage = postRepository.findAll(getSortedPaging(offset, limit, sort));
            postResponse.setCount(recentPage.getTotalElements());
            postResponse.setPosts(toPostDto(recentPage));
        }
        if (mode.equals("best")) {
            Page<Posts> popularPage = postRepository.findPostsOrderByLikes(getPaging(offset, limit));
            postResponse.setCount(popularPage.getTotalElements());
            postResponse.setPosts(toPostDto(popularPage));
        }
        if (mode.equals("popular")) {
            Page<Posts> bestPage = postRepository.findPostsOrderByComments(getPaging(offset, limit));
            postResponse.setCount(bestPage.getTotalElements());
            postResponse.setPosts(toPostDto(bestPage));
        }
        if (mode.equals("early")) {
            sort = Sort.by("time").ascending();
            Page<Posts> earlyPage = postRepository.findAll(getSortedPaging(offset, limit, sort));
            postResponse.setCount(earlyPage.getTotalElements());
            postResponse.setPosts(toPostDto(earlyPage));
        }
        return postResponse;
    }

    public PostResponse getPost(int offset, int limit, String query) {

        PostResponse postResponse = new PostResponse();

        if (query.isEmpty()) {
            sort = Sort.by("time").descending();
            Page<Posts> recentPage = postRepository.findAll(getSortedPaging(offset, limit, sort));
            postResponse.setCount(recentPage.getTotalElements());
            postResponse.setPosts(toPostDto(recentPage));
        } else {
            Page<Posts> postsPage = postRepository.findPostsByQuery(query, getPaging(offset, limit));
            postResponse.setCount(postsPage.getTotalElements());
            postResponse.setPosts(toPostDto(postsPage));
        }
        return postResponse;
    }

    public CalendarResponse getPostsByYear(String year) {

        List<Integer> years = new ArrayList<>();
        HashMap<String, Integer> posts = new HashMap<>();
        CalendarResponse calendarResponse = new CalendarResponse();

        Calendar currentDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        List<Posts> yearList = postRepository.findPostsByYear(currentDate);

        for (Posts post : yearList) {

            Calendar postTime = post.getTime();

            if (!years.contains(postTime.get(Calendar.YEAR))) {
                years.add(postTime.get(Calendar.YEAR));
            }

            calendarResponse.setYears(years);
        }

        int parsedYear = Integer.parseInt(year);

        Calendar endPoint = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Calendar startPoint = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        endPoint.set(Calendar.YEAR, parsedYear);
        endPoint.set(Calendar.MONTH, Calendar.DECEMBER);
        endPoint.set(Calendar.DAY_OF_MONTH, 31);

        startPoint.set(Calendar.YEAR, parsedYear);
        startPoint.set(Calendar.MONTH, Calendar.JANUARY);
        startPoint.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (Posts post : yearList) {

            Calendar postTime = post.getTime();

            if (postTime.before(endPoint) && (postTime.after(startPoint))) {
                String key = dateFormat.format(postTime.getTime());
                if (posts.containsKey(key)) {
                    posts.put(key,
                            posts.get(key) + 1);
                } else {
                    posts.put(key, 1);
                }
            }
        }

        calendarResponse.setPosts(posts);

        return calendarResponse;
    }

    public PostResponse getPostsByDate(int offset, int limit,
                                       String date) {

        PostResponse postResponse = new PostResponse();

        sort = Sort.by("time").descending();
        Page<Posts> page = postRepository.findAll(getSortedPaging(offset, limit, sort));
        List<Posts> postsList = page.getContent();
        List<Posts> dateList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat();
        dateFormat.applyPattern("yyyy-MM-dd");

        for (Posts post : postsList) {
            if (dateFormat.format(post.getTime().getTime()).equals(date)) {
                dateList.add(post);
            }
        }

        List<PostDto> postDtoList =
                dateList.stream().map(this::entityToDto).collect(Collectors.toList());

        postResponse.setCount(page.getTotalElements());
        postResponse.setPosts(postDtoList);
        return postResponse;
    }

    public PostResponse getPostsByTag(int offset, int limit, String tag) {

        PostResponse postResponse = new PostResponse();

        sort = Sort.by("time").descending();
        Page<Posts> tagPage = postRepository.findAll(getSortedPaging(offset, limit, sort));
        List<Posts> postsWithTagList = tagPage.getContent();

        List<Posts> finalList = new ArrayList<>();

        for (Posts post : postsWithTagList) {
            List<Tags> tagList = post.getTagsList();
            for (Tags postTag : tagList) {
                if (postTag.getName().equals(tag)) {
                    finalList.add(post);
                }
            }
        }
        List<PostDto> postDtoList =
                finalList.stream().map(this::entityToDto).collect(Collectors.toList());

        postResponse.setCount(finalList.size());
        postResponse.setPosts(postDtoList);

        return postResponse;
    }

    public PostByIdResponse getPostById(int id) {

        PostByIdResponse postByIdResponse = new PostByIdResponse();

        Optional<Posts> optionalPost = postRepository.findPostById(id);
        if (optionalPost.isPresent()) {
            Posts post = optionalPost.get();

            Date date = post.getTime().getTime();
            long unixTime = date.getTime() / 1000;

            List<PostVotes> postVotesList = post.getPostVoteList();

            List<PostComments> postComments = post.getPostCommentsList();
            List<Tags> tagsList = post.getTagsList();
            List<CommentDto> commentDtoList = new ArrayList<>();
            List<String> tags = new ArrayList<>();

            UserDto userDtoForPost = new UserDto();

            userDtoForPost.setId(post.getUser().getId());
            userDtoForPost.setName(post.getUser().getName());

            for (PostComments comment : postComments) {

                CommentDto commentDto = new CommentDto();
                CommentUserDto commentUserDto = new CommentUserDto();

                int commentId = comment.getId();
                long timestamp = comment.getTime().getTime() / 1000;
                String text = comment.getText();

                commentUserDto.setId(comment.getUser().getId());
                commentUserDto.setName(comment.getUser().getName());
                commentUserDto.setPhoto(comment.getUser().getPhoto());

                commentDto.setId(commentId);
                commentDto.setTimestamp(timestamp);
                commentDto.setText(text);
                commentDto.setUser(commentUserDto);

                commentDtoList.add(commentDto);
            }

            for (Tags tag : tagsList) {
                tags.add(tag.getName());
            }

            int viewCount = post.getViewCount();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof AnonymousAuthenticationToken)) {

                String currentUserName = authentication.getName();
                String authorName = post.getUser().getEmail();

                User user = (User) authentication.getPrincipal();
                main.model.User currentUser = userRepository.findByEmail(user.getUsername());

                boolean isModerator = currentUser.getIsModerator() == 1;
                boolean isAuthor = authorName.equals(currentUserName);

                if ((isAuthor) || (isModerator)) {
                    postByIdResponse.setViewCount(viewCount);
                } else {
                    postByIdResponse.setViewCount(++viewCount);
                }
                post.setViewCount(viewCount);
                Calendar setTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                setTime.setTime(post.getTime().getTime());
                post.setTime(setTime);
                postRepository.save(post);
            }

            postByIdResponse.setViewCount(viewCount);
            postByIdResponse.setId(post.getId());
            postByIdResponse.setTimestamp(unixTime);
            postByIdResponse.setActive(post.getIsActive());
            postByIdResponse.setUser(userDtoForPost);
            postByIdResponse.setTitle(post.getTitle());
            postByIdResponse.setText(post.getText());
            postByIdResponse.setLikeCount(countLikes(postVotesList));
            postByIdResponse.setDislikeCount(countDisLikes(postVotesList));

            postByIdResponse.setComments(commentDtoList);
            postByIdResponse.setTags(tags);
        }
        return postByIdResponse;
    }

    public PostResponse getModerationPosts(int offset, int limit, String status) {

        PostResponse postResponse = new PostResponse();
        ModerationStatus moderationStatus = ModerationStatus.NEW;
        if (status.equals("declined")) {
            moderationStatus = ModerationStatus.DECLINED;
        }
        if (status.equals("accepted")) {
            moderationStatus = ModerationStatus.ACCEPTED;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        main.model.User currentUser = userRepository.findByEmail(user.getUsername());

        sort = Sort.by("time").descending();
        Page<Posts> recentPage = postRepository
                .findModeratedPosts(currentUser.getId(), moderationStatus,
                        getSortedPaging(offset, limit, sort));
        postResponse.setCount(recentPage.getTotalElements());
        postResponse.setPosts(toPostDto(recentPage));

        return postResponse;
    }

    public GeneralResponse moderatePost(ModerateRequest moderateRequest, Principal principal) {
        GeneralResponse response = new GeneralResponse();

        main.model.User user = userRepository.findByEmail(principal.getName());
        Posts post = postRepository.findById(moderateRequest.getPostId());
        if (user.getIsModerator() == 1) {
            if (moderateRequest.getDecision().equals("accept")) {
                post.setModerationStatus(ModerationStatus.ACCEPTED);
                Calendar setTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                setTime.setTime(post.getTime().getTime());
                post.setTime(setTime);
            }
            if (moderateRequest.getDecision().equals("decline")) {
                post.setModerationStatus(ModerationStatus.DECLINED);
                Calendar setTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                setTime.setTime(post.getTime().getTime());
                post.setTime(setTime);
            }
            post.setModeratorId(user.getId());
            postRepository.save(post);
            response.setResult(true);
        } else {
            response.setResult(false);
        }
        return response;
    }

    public PostResponse getMyPosts(int offset, int limit, String status) {

        PostResponse postResponse = new PostResponse();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        main.model.User currentUser = userRepository.findByEmail(user.getUsername());

        if (status.equals("inactive")) {
            sort = Sort.by("time").descending();
            Page<Posts> inactivePosts = postRepository
                    .findInactivePosts(currentUser, getSortedPaging(offset, limit, sort));
            postResponse.setCount(inactivePosts.getTotalElements());
            postResponse.setPosts(toPostDto(inactivePosts));
        }
        if (status.equals("pending")) {
            sort = Sort.by("time").descending();
            Page<Posts> pendingPosts = postRepository
                    .findPendingPosts(currentUser, getSortedPaging(offset, limit, sort));
            postResponse.setCount(pendingPosts.getTotalElements());
            postResponse.setPosts(toPostDto(pendingPosts));
        }
        if (status.equals("declined")) {
            sort = Sort.by("time").descending();
            Page<Posts> declinedPosts = postRepository
                    .findDeclinedPosts(currentUser, getSortedPaging(offset, limit, sort));
            postResponse.setCount(declinedPosts.getTotalElements());
            postResponse.setPosts(toPostDto(declinedPosts));
        }
        if (status.equals("published")) {
            sort = Sort.by("time").descending();
            Page<Posts> publishedPosts = postRepository
                    .findPublishedPosts(currentUser, getSortedPaging(offset, limit, sort));
            postResponse.setCount(publishedPosts.getTotalElements());
            postResponse.setPosts(toPostDto(publishedPosts));
        }
        return postResponse;
    }

    public PostingResponse makePost(PostRequest postRequest, Principal principal) {

        main.model.User currentUser = userRepository.findByEmail(principal.getName());

        PostingResponse postingResponse = new PostingResponse();
        Posts post = new Posts();
        PostErrorDto postErrorDto = new PostErrorDto();

        if ((postRequest.getTitle().length() < MIN_TITLE_LENGTH) || (postRequest.getText().length()
                < MIN_TEXT_LENGTH)) {
            if (postRequest.getTitle().length() < MIN_TITLE_LENGTH) {
                postErrorDto.setTitle("Заголовок не установлен");
            }
            if (postRequest.getText().length() < MIN_TEXT_LENGTH) {
                postErrorDto.setText("Текст публикации слишком крорткий");
            }
            postingResponse.setResult(false);
            postingResponse.setErrors(postErrorDto);
        } else {

            Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar postTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            postTime.setTimeInMillis(postRequest.getTimestamp());

            if (postTime.before(currentTime)) {
                post.setTime(currentTime);
            } else {
                post.setTime(postTime);
            }

            GlobalSettings postPremoderation = globalSettingsRepository.findByCode("POST_PREMODERATION");
            if (postPremoderation.getValue().equals("YES")) {
                post.setModerationStatus(ModerationStatus.NEW);
            } else {
                post.setModerationStatus(ModerationStatus.ACCEPTED);
            }

            post.setIsActive(postRequest.getActive());
            post.setTitle(postRequest.getTitle());
            post.setText(postRequest.getText());
            post.setUser(currentUser);
            List<String> stringTags = postRequest.getTags();
            post.setModeratorId(moderatorId);
            postRepository.save(post);
            post.setTagsList(makeTagsList(stringTags, post));
            postRepository.save(post);
            postingResponse.setResult(true);
        }
        return postingResponse;
    }

    public PostingResponse updatePost(int id, PostRequest postRequest, Principal principal) {

        main.model.User currentUser = userRepository.findByEmail(principal.getName());

        PostingResponse postingResponse = new PostingResponse();
        Posts post = postRepository.findById(id);
        PostErrorDto postErrorDto = new PostErrorDto();

        if ((postRequest.getTitle().length() < MIN_TITLE_LENGTH) || (postRequest.getText().length()
                < MIN_TEXT_LENGTH)) {
            if (postRequest.getTitle().length() < MIN_TITLE_LENGTH) {
                postErrorDto.setTitle("Заголовок не установлен");
            }
            if (postRequest.getText().length() < MIN_TEXT_LENGTH) {
                postErrorDto.setText("Текст публикации слишком крорткий");
            }
            postingResponse.setResult(false);
            postingResponse.setErrors(postErrorDto);
        } else {
            Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            Calendar postTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            postTime.setTimeInMillis(postRequest.getTimestamp());

            if (postTime.before(currentTime)) {
                post.setTime(currentTime);
            } else {
                post.setTime(postTime);
            }
            post.setIsActive(postRequest.getActive());
            post.setTitle(postRequest.getTitle());
            post.setText(postRequest.getText());
            post.setUser(currentUser);
            List<String> stringTags = postRequest.getTags();
            post.setTagsList(makeTagsList(stringTags, post));

            boolean isAuthor = currentUser.getName().equals(post.getUser().getName());
            boolean isModerator = currentUser.getIsModerator() == 1;
            if (isAuthor) {
                post.setModerationStatus(ModerationStatus.NEW);
            }
            if (isModerator) {
                post.setModerationStatus(post.getModerationStatus());
            }

            postRepository.save(post);
            postingResponse.setResult(true);
        }
        return postingResponse;
    }

    public CommentResponse postComment(CommentRequest commentRequest, Principal principal) {

        main.model.User currentUser = userRepository.findByEmail(principal.getName());

        CommentResponse commentResponse = new CommentResponse();
        PostComments postComment = new PostComments();

        int parentId = commentRequest.getParentId();
        int postId = commentRequest.getPostId();
        String commentText = commentRequest.getText();

        Optional<Posts> optionalPost = postRepository.findPostById(postId);
        Calendar currentTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (optionalPost.isPresent()) {
            postComment.setPosts(optionalPost.get());
            postComment.setParentId(parentId);
            postComment.setUser(currentUser);
            postComment.setTime(currentTime.getTime());
            postComment.setText(commentText);

            postCommentRepository.save(postComment);
            commentResponse.setId(postComment.getId());
        }
        return commentResponse;
    }

    public GeneralResponse like(LikeDislikeRequest likeRequest, Principal principal) {

        main.model.User currentUser = userRepository.findByEmail(principal.getName());

        GeneralResponse generalResponse = new GeneralResponse();
        PostVotes postVote = new PostVotes();

        Posts post = postRepository.findById(likeRequest.getPostId());
        Optional<PostVotes> optionalPostVote = postVoteRepository.findByPostId(post,
                currentUser.getId());

        if (optionalPostVote.isEmpty()) {

            postVote.setPost(post);
            postVote.setUserId(currentUser.getId());
            postVote.setTime(new Date());
            postVote.setValue(1);

            postVoteRepository.save(postVote);
            generalResponse.setResult(true);
        }
        if (optionalPostVote.isPresent()) {
            PostVotes vote = optionalPostVote.get();
            int voteUserId = vote.getUserId();
            int currentUserId = currentUser.getId();
            if ((voteUserId == currentUserId) && (vote.getValue() == -1)) {
                vote.setValue(1);
                postVoteRepository.save(vote);
                generalResponse.setResult(true);
            } else {
                generalResponse.setResult(false);
            }
        }
        return generalResponse;
    }

    public GeneralResponse dislike(LikeDislikeRequest dislikeRequest, Principal principal) {

        main.model.User currentUser = userRepository.findByEmail(principal.getName());

        GeneralResponse generalResponse = new GeneralResponse();
        PostVotes postVote = new PostVotes();

        Posts post = postRepository.findById(dislikeRequest.getPostId());
        Optional<PostVotes> optionalPostVote = postVoteRepository.findByPostId(post,
                currentUser.getId());

        if (optionalPostVote.isEmpty()) {

            postVote.setPost(post);
            postVote.setUserId(currentUser.getId());
            postVote.setTime(new Date());
            postVote.setValue(-1);

            postVoteRepository.save(postVote);
            generalResponse.setResult(true);
        }
        if (optionalPostVote.isPresent()) {
            PostVotes vote = optionalPostVote.get();
            int voteUserId = vote.getUserId();
            int currentUserId = currentUser.getId();
            if ((voteUserId == currentUserId) && (vote.getValue() == 1)) {
                vote.setValue(-1);
                postVoteRepository.save(vote);
                generalResponse.setResult(true);
            } else {
                generalResponse.setResult(false);
            }
        }
        return generalResponse;
    }

    private PostDto entityToDto(Posts post) {

        PostDto postDto = new PostDto();
        UserDto userDto = new UserDto();

        userDto.setId(post.getUser().getId());
        userDto.setName(post.getUser().getName());

        List<PostComments> postCommentsList = post.getPostCommentsList();
        List<PostVotes> postVotesList = post.getPostVoteList();

        int countComments = postCommentsList.size();

        Date date = post.getTime().getTime();
        long unixTime = date.getTime() / 1000;

        String fullText = post.getText();
        String announce;

        if (fullText.length() > MAX_LENGTH) {
            String text = fullText.substring(0, MAX_LENGTH);
            announce = text + "...";
        } else {
            announce = fullText;
        }

        postDto.setId(post.getId());
        postDto.setTimeStamp(unixTime);
        postDto.setUserDto(userDto);
        postDto.setTitle(post.getTitle());
        postDto.setAnnounce(announce);
        postDto.setLikeCount(countLikes(postVotesList));
        postDto.setDislikeCount(countDisLikes(postVotesList));
        postDto.setCommentCount(countComments);
        postDto.setViewCount(post.getViewCount());

        return postDto;
    }

    public List<PostDto> toPostDto(Page<Posts> page) {
        List<Posts> postsList = page.getContent();
        List<PostDto> postDtoList =
                postsList.stream().map(this::entityToDto).collect(Collectors.toList());
        return postDtoList;
    }

    public List<Tags> makeTagsList(List<String> stringTags, Posts post) {
        List<Tags> tagList = new ArrayList<>();
        for (String tagName : stringTags) {
            Optional<Tags> optionalTag = tagRepository.findTagByName(tagName);
            if (optionalTag.isEmpty()) {
                Tags tag = new Tags();
                tag.setName(tagName);
                tagList.add(tag);
            } else {
                Tags repoTag = optionalTag.get();
                Tag2Post tag2Post = new Tag2Post();
                tag2Post.setTagId(repoTag.getId());
                tag2Post.setPostId(post.getId());
                tag2PostRepository.save(tag2Post);
            }
        }
        return tagList;
    }

    public Pageable getPaging(int offset, int limit) {
        Pageable paging;
        int pageNumber = offset / limit;
        paging = PageRequest.of(pageNumber, limit);

        return paging;
    }

    public Pageable getSortedPaging(int offset, int limit, Sort sort) {
        Pageable sortedPaging;
        int pageNumber = offset / limit;
        sortedPaging = PageRequest.of(pageNumber, limit, sort);

        return sortedPaging;
    }

    public int countLikes(List<PostVotes> postVotesList) {
        int countLikes = 0;
        for (PostVotes postVote : postVotesList) {
            int value = postVote.getValue();
            if (value == 1) {
                countLikes++;
            }
        }
        return countLikes;
    }

    public int countDisLikes(List<PostVotes> postVotesList) {
        int countDislikes = 0;
        for (PostVotes postVote : postVotesList) {
            int value = postVote.getValue();
            if (value == -1) {
                countDislikes++;
            }
        }
        return countDislikes;
    }
}
