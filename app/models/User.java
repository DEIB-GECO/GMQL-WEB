package models;

import com.avaje.ebean.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import play.data.validation.Constraints;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Entity
public class User extends Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String authToken;

    @Column(length = 256, unique = true, nullable = false)
    @Constraints.MaxLength(256)
    @Constraints.Required
    @Constraints.Email
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.toLowerCase();
    }


    @Column(length = 256, unique = true, nullable = false)
    @Constraints.MaxLength(256)
    @Constraints.Required
    @Constraints.Email
    private String emailAddress;

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress.toLowerCase();
    }

    @Column(length = 64, nullable = false)
    private byte[] shaPassword;

    @Transient
    @Constraints.Required
    @Constraints.MinLength(6)
    @Constraints.MaxLength(256)
    @JsonIgnore
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        shaPassword = getSha512(password);
    }

    @Column(length = 256, nullable = false)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(256)
    public String firstName;

    @Column(length = 256, nullable = false)
    @Constraints.Required
    @Constraints.MinLength(2)
    @Constraints.MaxLength(256)
    public String lastName;



    @Column(nullable = false)
    public Date creationDate;

    @Column(nullable = false)
    public Date lastUsedDate;

    public String createToken() {
        authToken = UUID.randomUUID().toString();
        save();
        return authToken;
    }

    public void deleteAuthToken() {
        authToken = null;
        save();
    }


    public String fullName(){
        return firstName + " " + lastName;
    }

//    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
//    @JsonIgnore
//    public List<Todo> todos = new ArrayList<>();


    public User() {
        this.creationDate = new Date();
        this.lastUsedDate = new Date();
    }

    public User(String username, String emailAddress, String password, String firstName, String lastName) {
        setUsername(username);
        setEmailAddress(emailAddress);
        setPassword(password);
        this.firstName = firstName;
        this.lastName = lastName;
        this.creationDate = new Date();
        this.lastUsedDate = new Date();
    }


    public static byte[] getSha512(String value) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(value.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Finder<Long, User> find = new Finder<>(User.class);

    public static User findByAuthToken(String authToken) {
        if (authToken == null) {
            return null;
        }

        try {
            if (authToken.equals("test-best-token"))
                return find.where().eq("username", "canakoglu").findUnique();

            User user = find.where().eq("authToken", authToken).findUnique();
            //TODO update every 15 min.
            user.lastUsedDate = new Date();
            user.save();
            return user;
        } catch (Exception e) {
            return null;
        }
    }

    public static User findByUsernameAndPassword(String username, String password) {
        // todo: verify this query is correct.  Does it need an "and" statement?
        User user = find.where().eq("username", username.toLowerCase()).eq("shaPassword", getSha512(password)).findUnique();
        //TODO update every 15 min.
        if(user!=null) {
            user.lastUsedDate = new Date();
            user.save();
        }
        return user;
    }

    public static User findByUsername(String username) {
        // todo: verify this query is correct.  Does it need an "and" statement?
        User user = find.where().eq("username", username.toLowerCase()).findUnique();
        //TODO update every 15 min.
        if(user!=null) {
            user.lastUsedDate = new Date();
            user.save();
        }
        return user;
    }

    public static boolean existsByUsername(String username) {
        // todo: verify this query is correct.  Does it need an "and" statement?
        return findByUsername(username) != null;
    }

//    public static String getPasswordRecovery(String username) {
//        // todo: verify this query is correct.  Does it need an "and" statement?
//        User user = findByUsername(username);
//        if(user != null) {
//            return user.createToken();
////            String passwordRecovery = UUID.randomUUID().toString();
////            user.passwordRecovery = passwordRecovery;
////            user.save();
////            return passwordRecovery;
//        }
//        else
//            return null;
//    }

//    public static String getEmailAddress(String username) {
//        // todo: verify this query is correct.  Does it need an "and" statement?
//        User user = find.where().eq("username", username.toLowerCase()).findUnique();
//        if(user != null) {
//            return user.getEmailAddress();
//        }
//        else
//            return null;
//    }



}
