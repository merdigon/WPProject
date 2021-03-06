package com.controllers;


import com.LibraryConfiguration.LibraryConfiguration;
import com.models.*;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;


@RestController
public class RestfulBookController extends BaseController {


    /**
     * Pobieranie wszystkich ksiazek. Dostep wszyscy
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/getBooks/", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> listAllbooks() {
        List<Book> books = bookDAO.getAll();
        if (books.isEmpty()) {
            return new ResponseEntity<List<Book>>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<Book>>(books, HttpStatus.OK);
    }

    /**
     * Sprawdzenie stanu ksiazki. Dostep wszyscy
     * @param request -"uuid"
     * @return stan ksiazki
     */
    @RequestMapping(value = "/rest/getCondition/", method = RequestMethod.GET)
    public ResponseEntity<Condition> condition(HttpServletRequest request) {
        String bookUuid = request.getParameter("uuid");
        Book book = bookDAO.get(bookUuid);
        if (book == null) {
            return new ResponseEntity<Condition>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Condition>(book.getCondition(), HttpStatus.OK);
    }


    /**
     * Zwraca ksiazke o podanych uuid. Dostep wszyscy.
     * @param request -"uuid"
     * @return ksiazka o podanym id
     */

    @RequestMapping(value = "/rest/getBook/", method = RequestMethod.GET)
    public ResponseEntity<Book> getBook(HttpServletRequest request) {
        String uuid = request.getParameter("uuid");

        Book book = bookDAO.get(uuid);
        if (book == null) {
            System.out.println("book with uuid " + uuid + " not found");
            return new ResponseEntity<Book>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<Book>(book, HttpStatus.OK);
    }



    /**
     * Usuwa ksiazke o podanym uuid. Tylko admin
     * @param request -"uuid", "token"
     * @return status
     */

    @RequestMapping(value = "/rest/deleteBook/", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteBook(HttpServletRequest request) {

        String token = request.getParameter("token");
        String uuid= request.getParameter("uuid");

        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<String>("{\"Status\" : \"Failure bad token\"}",HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<String>("{\"Status\" : \"Failure no permission\"}",HttpStatus.FORBIDDEN);

        Book book = bookDAO.get(uuid);
        if (book == null) {
            System.out.println("Unable to delete. Book with uuid " + uuid + " not found");
            return new ResponseEntity<String>("{\"Status\" : \"Failure book not found\"}",HttpStatus.NOT_FOUND);
        }

        bookDAO.delete(uuid);
        return new ResponseEntity<String>("{\"Status\" : \"Success\"}",HttpStatus.OK);
    }

    /**
     * Przenosi ksiazke o podanym uuid do innego dzialu. Tylko admin
     * @param request -"uuid", "token", "section"
     * @return ksiazka w postaci jsona
     */

    @RequestMapping(value = "/rest/moveBook/", method = RequestMethod.POST)
    public ResponseEntity<Book> moveBook(HttpServletRequest request) {

        String uuid = request.getParameter("uuid");
        String token = request.getParameter("token");
        String section = request.getParameter("section");
        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<Book>(HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<Book>(HttpStatus.FORBIDDEN);

        Book currentBook = bookDAO.get(uuid);

        if (currentBook == null) {
            System.out.println("Book with uuid " + uuid + " not found");
            return new ResponseEntity<Book>(HttpStatus.NOT_FOUND);
        }

        currentBook.setSection(sectionDAO.saveIfNotInDB(new Section(section)));
        bookDAO.update(currentBook);
        return new ResponseEntity<Book>(currentBook, HttpStatus.OK);
    }

    /**
     * Zmiana stanu ksiazki. Tylko admin
     * @param request -"uuid", "token", "condition"
     * @return zminiona ksiazka
     */

    @RequestMapping(value = "/rest/changeCondition/", method = RequestMethod.POST)
    public ResponseEntity<Book> changeCondition(HttpServletRequest request) {

        String token = request.getParameter("token");
        String uuid = request.getParameter("uuid");
        String condition = request.getParameter("condition");
        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<Book>(HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<Book>(HttpStatus.FORBIDDEN);

        Book book = bookDAO.get(uuid);

        if (book == null) {
            System.out.println("Book with uuid " + uuid + " not found");
            return new ResponseEntity<Book>(HttpStatus.NOT_FOUND);
        }

        Condition cond = conditionDAO.saveIfNotInDB(new Condition(Conditions.valueOf(condition)));

        book.setCondition(cond);
        bookDAO.update(book);
        return new ResponseEntity<Book>(book, HttpStatus.OK);
    }

    /**
     * Rezerwacja ksiazki. Tylko zalogowani uzytkownicy
     * @param request -"bookUuid", "token", "idNumber"
     * @return status
     */
    @RequestMapping(value = "/rest/reserveBook/", method = RequestMethod.POST)
    public ResponseEntity<UserModel> reserveBook(HttpServletRequest request) {
        String token = request.getParameter("token");
        String bookUuid= request.getParameter("bookUuid");
        int idNumber = Integer.parseInt(request.getParameter("idNumber"));
        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<UserModel>(HttpStatus.UNAUTHORIZED);


        Book book = bookDAO.get(bookUuid);
        UserModel user = userModelDAO.getByIdNumber(idNumber);
        if (book == null) {
            System.out.println("Book with uuid " + bookUuid + " not found");
            return new ResponseEntity<UserModel>(HttpStatus.NOT_FOUND);
        }

        if (user == null) {
            System.out.println("user with id " + idNumber + " not found");
            return new ResponseEntity<UserModel>(HttpStatus.NOT_FOUND);
        }

        if(user.getReservedBooks().size()+1>= LibraryConfiguration.getInstance().getMaxReservedBooks())
            return new ResponseEntity<UserModel>(HttpStatus.IM_USED);

        if (!book.getCondition().equals(Conditions.valueOf("Available")))
            return new ResponseEntity<UserModel>(HttpStatus.IM_USED);

        Condition condition = new Condition(Conditions.valueOf("Reserved"));
        book.setCondition(conditionDAO.saveIfNotInDB(condition));
        bookDAO.update(book);
        userModelDAO.addReservedBook(user, book);

        return new ResponseEntity<UserModel>(user, HttpStatus.OK);
    }

    /**
     * Anuluje rezerwacje ksiazki. wszyscy zalogowani maja dostep
     * @param request -"bookUuid", "token", "idNumber"
     * @return status
     */

    @RequestMapping(value = "/rest/cancelReservedBook/", method = RequestMethod.POST)
    public ResponseEntity<String> cancelReserveBook(HttpServletRequest request) {

        String token = request.getParameter("token");
        String bookUuid = request.getParameter("bookUuid");
        int idNumber= Integer.parseInt(request.getParameter("idNumber"));

        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<String>("{\"Status\" : \"Failure bad token\"}",HttpStatus.UNAUTHORIZED);


        Book book = bookDAO.get(bookUuid);
        UserModel user = userModelDAO.getByIdNumber(idNumber);
        if (book == null) {
            System.out.println("Book with uuid " + bookUuid + " not found");
            return new ResponseEntity<String>("{\"Status\" : \"Failure book not found\"}",HttpStatus.NOT_FOUND);
        }

        if (user == null) {
            System.out.println("user with id " + idNumber + " not found");
            return new ResponseEntity<String>("{\"Status\" : \"Failure user not found\"}",HttpStatus.NOT_FOUND);
        }

        if (!book.getCondition().equals(Conditions.valueOf("Reserved")))
            return new ResponseEntity<String>("{\"Status\" : \"Failure book is not reserved\"}",HttpStatus.IM_USED);


        Condition condition = new Condition(Conditions.valueOf("Available"));
        book.setCondition(conditionDAO.saveIfNotInDB(condition));
        bookDAO.update(book);
        userModelDAO.removeReservedBook(user, book);


        return new ResponseEntity<String>("{\"Status\" : \"Success\"}",HttpStatus.OK);
    }

    /**
     * zwraca liste wszystkich wypozyczonych ksiazkek uzytkownika. wszyscy zalogowani
     * @param request -"idNumber", "token"
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/getUserBooks/", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> listAllUserBooks(HttpServletRequest request) {

        String token = request.getParameter("token");
        int idNumber = Integer.parseInt(request.getParameter("idNumber"));

        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<List<Book>>(HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<List<Book>>(HttpStatus.FORBIDDEN);

        UserModel user = userModelDAO.getByIdNumber(idNumber);
        if (user == null) {
            System.out.println("user with idNumber " + idNumber+ " not found");
            return new ResponseEntity<List<Book>>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<List<Book>>(user.getBooks(), HttpStatus.OK);
    }

    /**
     * zwraca liste wszystkich wypozyczonych ksiazkek uzytkownika. wszyscy zalogowani
     * @param request -"userId"
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/getUserBooksById/", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> listAllUserBooksById(HttpServletRequest request) {

        String uuid = request.getParameter("userID");


        UserModel user = userModelDAO.get(uuid);
        if (user == null) {
            System.out.println("user with idNumber " + uuid+ " not found");
            return new ResponseEntity<List<Book>>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<List<Book>>(user.getBooks(), HttpStatus.OK);
    }

    /**
     * zwraca liste wszystkich wypozyczonych ksiazkek uzytkownika. wszyscy zalogowani
     * @param request -"idNumber", "token"
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/getUserBooksToken/", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> listAllUserBooksByToken(HttpServletRequest request) {

        String token = request.getParameter("token");

        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<List<Book>>(HttpStatus.UNAUTHORIZED);


        UserModel user = userModelDAO.getByLogin(session.getLogin());
        if (user == null) {
            return new ResponseEntity<List<Book>>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<List<Book>>(user.getBooks(), HttpStatus.OK);
    }

    /**
     *  zwraca liste wszystkich zarezerwowanych ksiazek uzytkownika. tylko admin
     * @param request -"idNumber", "token"
     * @return lista ksiazek
     */
    @RequestMapping(value = "/rest/getUserReservedBooks/", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> listAllUserReservedBooks(HttpServletRequest request) {

        String token = request.getParameter("token");
        int idNumber = Integer.parseInt(request.getParameter("idNumber"));

        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<List<Book>>(HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<List<Book>>(HttpStatus.FORBIDDEN);

        UserModel user = userModelDAO.getByIdNumber(idNumber);
        if (user == null) {
            System.out.println("user with uuid " + idNumber + " not found");
            return new ResponseEntity<List<Book>>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<List<Book>>(user.getReservedBooks(), HttpStatus.OK);
    }


    /**
     * Wypozyczenie ksiazki o podanych uuid. tylko admin
     * @param request -"bookUuid", "token", "idNumber"
     * @return status
     */

    @RequestMapping(value = "/rest/borrowBook/", method = RequestMethod.POST)
    public ResponseEntity<String> borrowBook(HttpServletRequest request) {

        String token = request.getParameter("token");
        String bookUuid= request.getParameter("bookUuid");
        int idNumber = Integer.parseInt(request.getParameter("idNumber"));

        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<String>("{\"Status\" : \"Failure bad session\"}",HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<String>("{\"Status\" : \"Failure no permission\"}",HttpStatus.FORBIDDEN);

        Book book = bookDAO.get(bookUuid);
        UserModel user = userModelDAO.getByIdNumber(idNumber);

        if (book == null) {
            System.out.println("Book with uuid " + bookUuid + " not found");
            return new ResponseEntity<String>("{\"Status\" : \"Failure book not found\"}",HttpStatus.NOT_FOUND);
        }


        if (user == null) {
            System.out.println("User with id " + idNumber + " not found");
            return new ResponseEntity<String>("{\"Status\" : \"Failed user not found\"}",HttpStatus.NOT_FOUND);
        }

        if(user.getBooks().size()+1>= LibraryConfiguration.getInstance().getMaxBorrowedBooks())
            return new ResponseEntity<String>("{\"Status\" : \"Failure you exceed the limit of borrowed books\"}",HttpStatus.IM_USED);

        if (!book.getCondition().equals(Conditions.valueOf("Available")))
            return new ResponseEntity<String>("{\"Status\" : \"Failure book not available\"}",HttpStatus.IM_USED);


        LocalDate planningReturnDate = new LocalDate().plusDays(LibraryConfiguration.getInstance().getBorrowedDays());


        BookDate date = new BookDate();
        date.setBorrowedDate(new LocalDate());
        date.setPlanningReturnDate(planningReturnDate);
        date.setLogin(user.getLogin());

        Condition condition = new Condition(Conditions.valueOf("Borrowed"));

        book.setCondition(conditionDAO.saveIfNotInDB(condition));
        book.addDate(dateDAO.saveIfNotInDB(date));
        bookDAO.update(book);
        userModelDAO.addBook(user, book);
        return new ResponseEntity<String>("{\"Status\" : \"Success\"}", HttpStatus.OK);
    }

    /**
     * Zwracanie wypozyczonej ksiazki tylko admin
     * @param request -"userUuid", "token", "idNumber"
     * @return httpstatus
     */

    @RequestMapping(value = "/rest/returnBook/", method = RequestMethod.POST)
    public ResponseEntity<String> returnBorrowBook(HttpServletRequest request) {

        String token = request.getParameter("token");
        String bookUuid = request.getParameter("bookUuid");


        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<String>("{\"Status\" : \"Failure bad token\"}",HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<String>("{\"Status\" : \"Failure no permission\"}",HttpStatus.FORBIDDEN);

        Book book = bookDAO.get(bookUuid);
        String login = book.getDates().get(book.getDates().size()-1).getLogin();
        UserModel user = userModelDAO.getByLogin(login);

        if (book == null) {
            System.out.println("Book with uuid " + bookUuid + " not found");
            return new ResponseEntity<String>("{\"Status\" : \"Failure book not found\"}",HttpStatus.NOT_FOUND);
        }


        if (!book.getCondition().equals(Conditions.valueOf("Borrowed")))
            return new ResponseEntity<String>("{\"Status\" : \"Failure book is not borrowed\"}",HttpStatus.IM_USED);

        BookDate date = bookDAO.getLastDate(book);

        double debt = 0;
        if (date.getPlanningReturnDate().isBefore(new LocalDate())) {
            debt = Days.daysBetween(date.getPlanningReturnDate(), new LocalDate()).getDays() * LibraryConfiguration.getInstance().getInterests();
        }

        user.addDebt(debt);
        user.removeBook(book);

        date.setReturnDate(new LocalDate());
        dateDAO.updateDate(date);

        Condition condition = new Condition(Conditions.valueOf("Available"));
        condition = conditionDAO.saveIfNotInDB(condition);
        book.setCondition(condition);
        bookDAO.update(book);
        userModelDAO.update(user);
        return new ResponseEntity<String>("{\"Status\" : \"Success\"}",HttpStatus.OK);
    }

    /**
     * Pobieranie wszystkich ksiazek z danej sekcji. Dostep wszyscy
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/getBooksFromSection/", method = RequestMethod.GET)
    public ResponseEntity<List<Book>> listAllbooksSection(HttpServletRequest request) {
        List<Book> books = bookDAO.getAllBySection(request.getParameter("section"));
        if (books.isEmpty()) {
            return new ResponseEntity<List<Book>>(HttpStatus.NO_CONTENT);
        }

        List<Book>booksNoInvetoried = new ArrayList<>();

        for(Book book : books){
            if(book.getIsInventoried()==false)
                booksNoInvetoried.add(book);
        }
        return new ResponseEntity<List<Book>>(booksNoInvetoried, HttpStatus.OK);
    }

    /**
     * Pobieranie wszystkich sekcji. Dostep wszyscy
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/getAllSection/", method = RequestMethod.GET)
    public ResponseEntity<List<Section>> listAllSection() {

        List<Section> sections = sectionDAO.getAll();
        List<Section> sectionsWithBooks = new ArrayList<>();
        for(Section section : sections){
            if(!bookDAO.getAllBySection(section.getName()).isEmpty())
                sectionsWithBooks.add(section);
        }

        return new ResponseEntity<List<Section>>(sectionsWithBooks, HttpStatus.OK);
    }

    /**
     * inwentaryzacja ksiazek
     * @return lista ksiazek
     */

    @RequestMapping(value = "/rest/inventory/", method = RequestMethod.GET)
    public ResponseEntity<String> listAllSection(HttpServletRequest request) {

        Book book = bookDAO.get(request.getParameter("bookId"));
        String state = request.getParameter("state");
        String token = request.getParameter("token");
        Session session = sessionManager.getAndUpdateSession(token);

        if (session == null)
            return new ResponseEntity<String>("{\"Status\" : \"Failure bad token\"}",HttpStatus.UNAUTHORIZED);

        if(!userModelDAO.getByLogin(session.getLogin()).getUserRole().getType().equals("ADMIN"))
            return new ResponseEntity<String>("{\"Status\" : \"Failure no permission\"}",HttpStatus.FORBIDDEN);

        if (book == null)
            return new ResponseEntity<String>("{\"Status\" : \"Failure no book with this uuid\"}",HttpStatus.NO_CONTENT);

        if(!state.equals("null")){
            if(state.equals("ok")){
            book.setIsInventoried(true);
                bookDAO.update(book);
            return new ResponseEntity<String>("{\"Status\" : \"Success\"}", HttpStatus.OK);
            } else{
                Condition condition;

                for(Condition condition1 : conditionDAO.getAll()){
                    if(condition1.getCondition().name().equals(state)){
                        book.setCondition(condition1);
                        book.setIsInventoried(true);
                        bookDAO.update(book);
                        return new ResponseEntity<String>("{\"Status\" : \"Success\"}", HttpStatus.OK);
                    }
                }

                return new ResponseEntity<String>("{\"Status\" : \"Failure bad condition\"}", HttpStatus.OK);
            }
        }

        return new ResponseEntity<String>("{\"Status\" : \"Failure no condition\"}", HttpStatus.OK);
    }


}