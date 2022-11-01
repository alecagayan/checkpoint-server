package server;

import java.util.Date;

public class Token {

    private String username;
    public Date issueDate;
    public Date expiryDate;
    public String role;
    public String org;

    public Token (String username, Date issueDate, Date expiryDate, String role, String org) {
        this.username = username;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
        this.role = role;
        this.org = org;
    }

    public String getUsername() {
        return username;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public boolean isExpired() {
        return expiryDate.before(new Date());
    }

    public String getRole() {
        return role;
    }

    public String getOrgId() {
        return org;
    }

    public String toString() {
        return "v1|" + username + "|" + issueDate.getTime() + "|" + expiryDate.getTime() + "|" + role + "|" + org + "|v1";
    }
    
}
