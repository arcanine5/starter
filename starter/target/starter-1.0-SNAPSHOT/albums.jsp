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
<% final int MAX_ROW_LEN = 4; %>

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
    to create an album or view a private album.</p>
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
<p> Displaying Album names here </p>
<table style="width:100%">
  <tr>
<% 
    for (int idx = 0; idx < albums.size(); idx++) {
      Album curr = albums.get(idx);
    
      // Insert row breaks every few albums
      if ((idx % MAX_ROW_LEN) == 0) {
        %> </tr> <tr>  <%
      }


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
    <td>
      <a href = "/images.jsp?albumName=<%= curr.getName() %>">
        <img src="<%= thumbnailSrc %>" alt="<%= curr.getName() %>" width = 250>
      </a>
      <p style="font-size:13px"> <b> <%= curr.getName() %> </b> </p>
      <p style="font-size:10px; color:darkgray"> <i> <%= allInAlb.count() %> Items </i> </p>
    </td>
    <br>
    <%

    }
%>
  </tr>
</table>

<%
    if (user == null) {
%>
<p> <i> Sign in to create an album. </i> </p>
<%
    } else {
%>
   <hr>


<%-- //[END datastore]--%>



<% // Create an Album form --%>
<div style="text-align:center;">

<p> <b> Create an Album </b> <p>
<form action="/create" method="post" name="createAlbum" id="createAlbum">
          <div>
            New Album Name: <input type="text" id="newAlbumName"   name="newAlbumName" placeholder="Vacation Pictures" required />
            <br>
            <input type="submit"  value="Create Album" />
          </div>
</form>
</div>
<%
    }
%>


<script>


</script>


</body>
</html>
<%-- //[END all]--%>
