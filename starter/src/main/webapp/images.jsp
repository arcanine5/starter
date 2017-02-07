<%-- //[START all]--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<%-- //[START imports]--%>
<%@ page import="com.example.starter.Post" %>
<%@ page import="com.example.starter.Guestbook" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.googlecode.objectify.ObjectifyService" %>
<%-- //[END imports]--%>

<%@ page import="java.util.List" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
</head>

<body>

<%
    String guestbookName = request.getParameter("guestbookName");
    if (guestbookName == null) {
        guestbookName = "default";
    }
    pageContext.setAttribute("guestbookName", guestbookName);
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
        pageContext.setAttribute("user", user);
%>

<p>Hello, ${fn:escapeXml(user.nickname)}! (You can
    <a href="<%= userService.createLogoutURL(request.getRequestURI()) %>">sign out</a>.)</p>
<%
    } else {
%>
<p>Hello!
    <a href="<%= userService.createLoginURL(request.getRequestURI()) %>">Sign in</a>
    to include your name with greetings you post.</p>
<%
    }
%>

<%-- //[START datastore]--%>
<%
    // Create the correct Ancestor key
      Key<Guestbook> theBook = Key.create(Guestbook.class, guestbookName);

    // Run an ancestor query to ensure we see the most up-to-date
    // view of the Greetings belonging to the selected Guestbook.
      List<Post> greetings = ObjectifyService.ofy()
          .load()
          .type(Post.class) // We want only Greetings
          .ancestor(theBook)    // Anyone in this book
          .order("-date")       // Most recent first - date is indexed.
          .limit(5)             // Only show 5 of them.
          .list();

    if (greetings.isEmpty()) {
%>
<p>Guestbook '${fn:escapeXml(guestbookName)}' has no messages.</p>
<%
    } else {
%>
<p>Messages in Guestbook '${fn:escapeXml(guestbookName)}'.</p>
<%
      // Display all Image posts
        for (Post greeting : greetings) {
            pageContext.setAttribute("greeting_content", greeting.content);
            String author;
            if (greeting.author_email == null) {
                author = "An anonymous person";
            } else {
                author = greeting.author_email;
                String author_id = greeting.author_id;
                if (user != null && user.getUserId().equals(author_id)) {
                    author += " (You)";
                }
            }
            pageContext.setAttribute("greeting_user", author);
            // Construct image URL
%>
<p><b>${fn:escapeXml(greeting_user)}</b> wrote:</p>
<blockquote>${fn:escapeXml(greeting_content)}</blockquote>
<img src="<%= greeting.imageFilename %>">
<%
        }
    }
%>

<%-- // Post Greeting / Upload file form --%>
<%  
    if (user == null) {
%>
<p> <i> Sign in to make a post </i> </p>
<%
    } else {
%>

<h2> Upload an image </h2>
<form action="/sign" method="post" name="putFile" id="putFile">
    <div><textarea name="content" rows="3" cols="60"></textarea></div>
    <div><input type="submit" value="Post Greeting"/></div>
    <input type="hidden" name="guestbookName" value="${fn:escapeXml(guestbookName)}"/>
    <div>
            Bucket: <input type="text" name="bucket" />
            File Name: <input type="text" name="fileName" />
            <br /> File Contents:
            
            <input type="file" name="pic" id="pic" accept="image/*">
            <br />
            <input type="submit" onclick='uploadFile(this)' value="Upload Content" />
    </div>

</form>
<%
    }
%>

<%-- //[END datastore]--%>
<hr>
<%-- // Switch Guestbook Form --%>
<form action="/images.jsp" method="get">
    <div><input type="text" name="guestbookName" value="${fn:escapeXml(guestbookName)}"/></div>
    <div><input type="submit" value="Switch Guestbook"/></div>
    <div> <input type="button" value="Added button" onclick='uploadFile(this)'/>  </div>
</form>

<% // Working upload file form --%>
<form action="/index.html" enctype="multipart/form-data" method="get" name="putFile" id="putFile">
          <div>
            Bucket: <input type="text" name="bucket" />
            File Name: <input type="text" name="fileName" />
            <br /> File Contents: <br />
            <!--<textarea name="content" id="content" rows="3" cols="60"></textarea> -->
            <input type="file" name="pic" id="pic" accept="image/*">
            <br />
            <input type="submit" onclick='uploadFile(this)' value="Upload Content" />
          </div>
</form>


<script>
  function myFunc() {
  
    alert("in myFunc()");
  
  }
  
  function uploadFile() {
        alert("Running uploadFile() javascript");

        var bucket = document.forms["putFile"]["bucket"].value;
        var filename = document.forms["putFile"]["fileName"].value;
        var gBookName = document.forms["putFile"]["guestbookName"].value;

        if (bucket == null || bucket == "" || filename == null || filename == "") {
          alert("Both Bucket and FileName are required griowjhgwg!!!!");
          return false;
        } else {
          alert("other");
          //var postData = document.forms["putFile"]["content"].value;
          //document.getElementById("content").value = null;

          var uploadButton = document.getElementById("pic");
          var theFile = uploadButton.files[0];
          alert("file name is: " + theFile.name);
          var postData = theFile;

          var request = new XMLHttpRequest();
          request.open("POST", "/gcs/" + bucket + "/" + filename + "?guestbookName=" + gBookName, false);
          request.setRequestHeader("Content-Type", "image/gif");
          request.send(postData);

          alert("done")
        }
      }
  

</script>


</body>
</html>
<%-- //[END all]--%>
