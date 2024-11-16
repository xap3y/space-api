<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
    <title>Image upload</title>
</head>
<body>
<h1>Upload Image</h1>
    <form:form action = "${pageContext.request.contextPath}/v1/image/upload" method="post" enctype="multipart/form-data">


        <label for="key">API Key:</label>
        <input type="text" id="key" name="key" required>
        <br><br>

        <label for="file">Choose file:</label>
        <input type="file" id="file" name="file" accept="image/*" required>
        <br><br>

        <button type="submit">Upload</button>
    </form:form>
</body>
</html>