package server;

import java.util.Date;

public class PasswordResetToken {

    private String email;
    public Date issueDate;
    public Date expiryDate;

    public PasswordResetToken (String email, Date issueDate, Date expiryDate) {
        this.email = email;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
    }

    public String getEmail() {
        return email;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public boolean isExpired() {
        return expiryDate.before(new Date());
    }

    public String toString() {
        return "p1|" + email + "|" + issueDate.getTime() + "|" + expiryDate.getTime() + "|p1";
    }
    
}
