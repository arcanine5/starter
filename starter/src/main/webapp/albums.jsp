<%-- //[START all]--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<%-- //[START imports]--%>
<%@ page import="com.example.starter.Post" %>
<%@ page import="com.example.starter.Album" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.googlecode.objectify.ObjectifyService" %>
<%@ page import="com.googlecode.objectify.cmd.SimpleQuery" %>

<%@ page import="com.example.starter.ImagesServlet" %>

<%-- //[END imports]--%>

<%@ page import="java.util.List" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <link type="text/css" rel="stylesheet" href="/stylesheets/albums.css"/>
    <title> Albums </title>
</head>

<body>

<h1> Albums Page </h1>

<%
    String albumName = request.getParameter("albumName");
    if (albumName == null) {
        albumName = "default";
    }
    pageContext.setAttribute("albumName", albumName);
    UserService userService = UserServiceFactory.getUserService();
    User user = userService.getCurrentUser();
    if (user != null) {
        pageContext.setAttribute("user", user);
%>

<p>Hello, ${fn:escapeXml(user.nickname)}! (You can
    <a href="<%= userService.createLogoutURL(request.getRequestURI() + "?albumName=" + albumName ) %>">sign out</a>.)</p>
<%
    } else {
%>
<p>Hello!
    <a href="<%= userService.createLoginURL(request.getRequestURI() + "?albumName=" + albumName ) %>">Sign in</a>
    to include your name with greetings you post.</p>
<%
    }
%>

<%-- //[START datastore]--%>
<%
    // Create the correct Ancestor key
    Key<Album> theBook = Key.create(Album.class, albumName);
    List<Album> albums = ObjectifyService.ofy()
        .load()
        .type(Album.class)
        .list();
    System.out.println(" albumbs Jsp found this many albums: " + albums.size());
   

   %>
<div style="text-align:center;">
<p><h1> Album '${fn:escapeXml(albumName)}' has no messages. </h1></p>

<hr>
<br>
</div>

<%-- Display Albums by name --%>
Displaying Album names here
<% 
    for (Album curr : albums) {
      // Use any contained image as a thumbnail for this album
      SimpleQuery<Post> allInAlb = ObjectifyService.ofy()
          .load()
          .type(Post.class) // We want only Greetings
          .ancestor(curr);    // Anyone in this album
      Post postFromAlb = allInAlb.first().now();

      String thumbnailSrc;
      if (postFromAlb == null) {
        thumbnailSrc = "/grey.png";
      } else {
        thumbnailSrc = postFromAlb.imageFilename + (ImagesServlet.APPEND_UUID_TO_FILENAME ? postFromAlb.getUuidString() : "");
      }

    %>
    <p> <%= curr.toString() %> </p>
    <a href = "/images.jsp?albumName=<%= curr.getName() %>">
      <img src="<%= thumbnailSrc %>" alt="<%= curr.getName() %>" width = 250>
    </a>
    <p style="font-size:12px"> <b> <%= curr.getName() %> </b> </p>
    <p style="font-size:9px; color:darkgray"> <i> <%= allInAlb.count() %> Items </i> </p>
    
    <br>
    <%

    }



    if (user == null) {
%>
<p> <i> Sign in to make a post </i> </p>
<%
    } else {
%>


<%
    }
%>

<%-- //[END datastore]--%>
<hr>

<%-- // Switch Album Form --%>
<form action="/images.jsp" method="get">
    <div><input type="text" name="albumName" value="${fn:escapeXml(albumName)}"/></div>
    <div><input type="submit" value="Switch Album"/></div>
    <div> <input type="button" value="Added button" onclick='uploadFile(this)'/>  </div>
</form>

<% // Create an Album form --%>
<div style="text-align:center;">

<p> <b> Create an Album </b> <p>
<form action="/create" method="post" name="createAlbum" id="createAlbum">
          <div>
            New Album Name: <input type="text" id="newAlbumName"   name="newAlbumName" required />
            <br>
            <input type="submit"  value="Create Album" />
          </div>
</form>
</div>


<script>


</script>


</body>
</html>
<%-- //[END all]--%>
