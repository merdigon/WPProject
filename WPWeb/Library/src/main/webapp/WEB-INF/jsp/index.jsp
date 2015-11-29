<%@taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>

<html>
<head>
    <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.11.3/jquery.min.js"></script>
    <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <title>Spring Page Redirection</title>
</head>

<body>
<h2>Library</h2>
<%@include file="partOfPage/buttons/loginRegistrationButton.jsp"%>

<div class="panel panel-primary">
    <div class="panel-heading">Book operations</div>
    <div class="panel-body">
        <sec:authorize access="hasRole('ADMIN')">

        </sec:authorize>
        <input type="button" class="btn btn-default" onclick="location.href='/showBooks'" value="Show books">
        <input type="button" class="btn btn-default" onclick="location.href='/searchBooks'" value="Search books">

    </div>
</div>
<sec:authorize access="hasRole('ADMIN')">
<div class="panel panel-primary">
    <div class="panel-heading">Admin tools</div>
    <div class="panel-body">
        <input type="button" class="btn btn-default" onclick="location.href='/admin/addBook'" value="Add book">
        <input type="button" class="btn btn-default" onclick="location.href='/admin/addSection'" value="Add section">
        <input type="button" class="btn btn-default" onclick="location.href='/admin/addType'" value="Add type of book">
        <input type="button" class="btn btn-default" onclick="location.href='/admin/showUsers'" value="Show users">
        <input type="button" class="btn btn-default" onclick="location.href='/admin/searchUser'" value="Search users">
        <input type="button" class="btn btn-default" onclick="location.href='/admin/configureLibrary'" value="Configure Library">

    </div>
</div>

</sec:authorize>


</body>
</html>