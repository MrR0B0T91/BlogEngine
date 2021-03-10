package main.model;

import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Entity
@Table(name = "users")
public class Users {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private int id;

  @Column(name = "is_moderator", nullable = false, columnDefinition = "TINYINT")
  private boolean isModerator;

  @Column(name = "reg_time", nullable = false, columnDefinition = "DATETIME")
  private Date regTime;

  @Column(nullable = false, columnDefinition = "VARCHAR(255)")
  private String name;

  @Column(nullable = false, columnDefinition = "VARCHAR(255)")
  private String email;

  @Column(nullable = false, columnDefinition = "VARCHAR(255)")
  private String password;

  @Column(columnDefinition = "VARCHAR(255)")
  private String code;

  @Column(columnDefinition = "TEXT")
  private String photo;

  @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private List<Posts> posts;

  @ManyToMany(mappedBy = "usersVoteList", fetch = FetchType.LAZY)
  private List<Posts> postsWithVotes;

  @ManyToMany(mappedBy = "postCommentsList", fetch = FetchType.LAZY)
  private List<Posts> postsWithComments;
}
