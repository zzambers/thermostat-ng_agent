/*
 * Copyright 2012, 2013 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.web.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.Configuration;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.ParsedStatement;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.PreparedParameters;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Put;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.Operator;
import com.redhat.thermostat.web.common.ExpressionSerializer;
import com.redhat.thermostat.web.common.OperatorSerializer;
import com.redhat.thermostat.web.common.PreparedParameterSerializer;
import com.redhat.thermostat.web.common.StorageWrapper;
import com.redhat.thermostat.web.common.ThermostatGSONConverter;
import com.redhat.thermostat.web.common.WebInsert;
import com.redhat.thermostat.web.common.WebPreparedStatement;
import com.redhat.thermostat.web.common.WebPreparedStatementResponse;
import com.redhat.thermostat.web.common.WebPreparedStatementSerializer;
import com.redhat.thermostat.web.common.WebQueryResponse;
import com.redhat.thermostat.web.common.WebQueryResponseSerializer;
import com.redhat.thermostat.web.common.WebRemove;
import com.redhat.thermostat.web.common.WebUpdate;
import com.redhat.thermostat.web.server.auth.Roles;
import com.redhat.thermostat.web.server.auth.WebStoragePathHandler;

@SuppressWarnings("serial")
public class WebStorageEndPoint extends HttpServlet {

    static final String CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX = "thermostat-cmdc-grant-";
    private static final String TOKEN_MANAGER_TIMEOUT_PARAM = "token-manager-timeout";
    private static final String TOKEN_MANAGER_KEY = "token-manager";

    // our strings can contain non-ASCII characters. Use UTF-8
    // see also PR 1344
    private static final String RESPONSE_JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private static final Logger logger = LoggingUtils.getLogger(WebStorageEndPoint.class);

    private Storage storage;
    private Gson gson;

    public static final String STORAGE_ENDPOINT = "storage.endpoint";
    public static final String STORAGE_USERNAME = "storage.username";
    public static final String STORAGE_PASSWORD = "storage.password";
    public static final String STORAGE_CLASS = "storage.class";
    
    private int currentCategoryId;

    private Map<String, Integer> categoryIds;
    private Map<Integer, Category<?>> categories;
    
    private Map<StatementDescriptor<?>, PreparedStatementHolder<?>> preparedStmts;
    private Map<Integer, PreparedStatementHolder<?>> preparedStatementIds;
    // Lock to be held for setting/getting prepared queries in the above maps
    private Object preparedStmtLock = new Object();
    private int currentPreparedStmtId;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        logger.log(Level.INFO, "Initializing web service");
        
        // check if thermostat home is set and readable
        checkThermostatHome();
        
        gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Pojo.class,
                        new ThermostatGSONConverter())
                .registerTypeHierarchyAdapter(Expression.class,
                        new ExpressionSerializer())
                .registerTypeHierarchyAdapter(Operator.class,
                        new OperatorSerializer())
                .registerTypeAdapter(WebQueryResponse.class, new WebQueryResponseSerializer<>())
                .registerTypeAdapter(PreparedParameter.class, new PreparedParameterSerializer())
                .registerTypeAdapter(WebPreparedStatement.class, new WebPreparedStatementSerializer())
                .create();
        categoryIds = new HashMap<>();
        categories = new HashMap<>();
        preparedStatementIds = new HashMap<>();
        preparedStmts = new HashMap<>();
        TokenManager tokenManager = new TokenManager();
        String timeoutParam = getInitParameter(TOKEN_MANAGER_TIMEOUT_PARAM);
        if (timeoutParam != null) {
            tokenManager.setTimeout(Integer.parseInt(timeoutParam));
        }
        getServletContext().setAttribute(TOKEN_MANAGER_KEY, tokenManager);
    }
    
    @Override
    public void destroy() {
        logger.log(Level.INFO, "Going to shut down web service");
        if (storage != null) {
            // See IcedTea BZ#1315. Shut down storage in order
            // to avoid further memory leaks.
            Connection connection = storage.getConnection();
            try {
                // Tests have null connections
                if (connection != null) {
                    connection.disconnect();
                }
            } finally {
                storage.shutdown();
            }
        }
        logger.log(Level.INFO, "Web service shut down finished");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        if (storage == null) {
            String storageClass = getServletConfig().getInitParameter(STORAGE_CLASS);
            String storageEndpoint = getServletConfig().getInitParameter(STORAGE_ENDPOINT);
            String username = getServletConfig().getInitParameter(STORAGE_USERNAME);
            String password = getServletConfig().getInitParameter(STORAGE_PASSWORD);
            storage = StorageWrapper.getStorage(storageClass, storageEndpoint, username, password);
        }
        String uri = req.getRequestURI();
        int lastPartIdx = uri.lastIndexOf("/");
        String cmd = uri.substring(lastPartIdx + 1);
        if (cmd.equals("prepare-statement")) {
            prepareStatement(req, resp);
        }
        else if (cmd.equals("query-execute")) {
            queryExecute(req, resp);
        } else if (cmd.equals("put-pojo")) {
            putPojo(req, resp);
        } else if (cmd.equals("register-category")) {
            registerCategory(req, resp);
        } else if (cmd.equals("remove-pojo")) {
            removePojo(req, resp);
        } else if (cmd.equals("update-pojo")) {
            updatePojo(req, resp);
        } else if (cmd.equals("get-count")) {
            getCount(req, resp);
        } else if (cmd.equals("save-file")) {
            saveFile(req, resp);
        } else if (cmd.equals("load-file")) {
            loadFile(req, resp);
        } else if (cmd.equals("purge")) {
            purge(req, resp);
        } else if (cmd.equals("ping")) {
            ping(req, resp);
        } else if (cmd.equals("generate-token")) {
            generateToken(req, resp);
        } else if (cmd.equals("verify-token")) {
            verifyToken(req, resp);
        }
    }
    
    private boolean isThermostatHomeSet() {
        try {
            // this throws config exception if neither the property
            // nor the env var is set
            new Configuration();
            return true;
        } catch (InvalidConfigurationException e) {
            return false;
        }
    }
    
    private void checkThermostatHome() {
        if (!isThermostatHomeSet()) {
            String msg = "THERMOSTAT_HOME context parameter not set!";
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        File thermostatHomeFile = getThermostatHome();
        if (!thermostatHomeFile.canRead()) {
            // This is bad news. If we can't at least read THERMOSTAT_HOME
            // we are bound to fail in some weird ways at some later point.
            String msg = "THERMOSTAT_HOME = "
                    + thermostatHomeFile.getAbsolutePath()
                    + " is not readable or does not exist!";
            logger.log(Level.SEVERE, msg);
            throw new RuntimeException(msg);
        }
        logger.log(Level.FINEST, "THERMOSTAT_HOME == "
                + thermostatHomeFile.getAbsolutePath());
    }

    private File getThermostatHome() {
        try {
            Configuration config = new Configuration();
            return new File(config.getThermostatHome());
        } catch (InvalidConfigurationException e) {
            // we should have just checked if this throws any exception
            logger.log(Level.SEVERE, "Illegal configuration!", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @WebStoragePathHandler( path = "prepare-statement" )
    private <T extends Pojo> void prepareStatement(HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.PREPARE_STATEMENT)) {
            return;
        }
        String queryDescrParam = req.getParameter("query-descriptor");
        String categoryIdParam = req.getParameter("category-id");
        Integer catId = gson.fromJson(categoryIdParam, Integer.class);
        Category<?> cat = getCategoryFromId(catId);
        WebPreparedStatementResponse response = new WebPreparedStatementResponse();
        if (cat == null) {
            // bad category? we refuse to accept this
            logger.log(Level.WARNING, "Attepted to prepare a statement with an illegal category id");
            response.setStatementId(WebPreparedStatementResponse.ILLEGAL_STATEMENT);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            return;
        }
        StatementDescriptor<?> desc = new StatementDescriptor<>(cat, queryDescrParam);
        synchronized (preparedStmtLock) {
            // see if we've prepared this query already
            if (preparedStmts.containsKey(desc)) {
                PreparedStatementHolder<T> holder = (PreparedStatementHolder<T>) preparedStmts
                        .get(desc);
                ParsedStatement<T> parsed = holder.getStmt()
                        .getParsedStatement();
                int freeVars = parsed.getNumParams();
                response.setNumFreeVariables(freeVars);
                response.setStatementId(holder.getId());
                writeResponse(resp, response,
                        WebPreparedStatementResponse.class);
                return;
            }
            // TODO: Check if descriptor is trusted (i.e. known)
            
            // Prepare the target statement and put it into our prepared statement
            // maps.
            PreparedStatement<T> targetPreparedStatement;
            try {
                targetPreparedStatement = (PreparedStatement<T>) storage
                        .prepareStatement(desc);
            } catch (DescriptorParsingException e) {
                logger.log(Level.WARNING, "Descriptor parse error!", e);
                response.setStatementId(WebPreparedStatementResponse.DESCRIPTOR_PARSE_FAILED);
                writeResponse(resp, response,
                        WebPreparedStatementResponse.class);
                return;
            }
            PreparedStatementHolder<T> holder = new PreparedStatementHolder<T>(
                    currentPreparedStmtId, targetPreparedStatement,
                    (Class<T>) cat.getDataClass());
            preparedStmts.put(desc, holder);
            preparedStatementIds.put(currentPreparedStmtId, holder);
            ParsedStatement<?> parsed = targetPreparedStatement
                    .getParsedStatement();
            response.setNumFreeVariables(parsed.getNumParams());
            response.setStatementId(currentPreparedStmtId);
            writeResponse(resp, response, WebPreparedStatementResponse.class);
            currentPreparedStmtId++;
        }
    }

    @WebStoragePathHandler( path = "ping" )
    private void ping(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.LOGIN)) {
            return;
        }
        
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "purge" )
    private void purge(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.PURGE)) {
            return;
        }
        
        String agentId = req.getParameter("agentId");
        storage.purge(agentId);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "load-file" )
    private void loadFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.LOAD_FILE)) {
            return;
        }
        
        String name = req.getParameter("file");
        try (InputStream data = storage.loadFile(name)) {
            if (data == null) {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            OutputStream out = resp.getOutputStream();
            byte[] buffer = new byte[512];
            int read = 0;
            while (read >= 0) {
                read = data.read(buffer);
                if (read > 0) {
                    out.write(buffer, 0, read);
                }
            }
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    @WebStoragePathHandler( path = "save-file" )
    private void saveFile(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (! isAuthorized(req, resp, Roles.SAVE_FILE)) {
            return;
        }
        
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if (! isMultipart) {
            throw new ServletException("expected multipart message");
        }
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            @SuppressWarnings("unchecked")
            List<FileItem> items = upload.parseRequest(req);
            for (FileItem item : items) {
                String fieldName = item.getFieldName();
                if (fieldName.equals("file")) {
                    String name = item.getName();
                    InputStream in = item.getInputStream();
                    storage.saveFile(name, in);
                }
            }
        } catch (FileUploadException ex) {
            throw new ServletException(ex);
        }
        
    }

    @WebStoragePathHandler( path = "get-count" )
    private void getCount(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.GET_COUNT)) {
            return;
        }
        try {
            String categoryParam = req.getParameter("category");
            int categoryId = gson.fromJson(categoryParam, Integer.class);
            Category<?> category = getCategoryFromId(categoryId);
            long result = storage.getCount(category);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType(RESPONSE_JSON_CONTENT_TYPE);
            gson.toJson(result, resp.getWriter());
            resp.flushBuffer();
        } catch (IOException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @WebStoragePathHandler( path = "register-category" )
    private synchronized void registerCategory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.REGISTER_CATEGORY)) {
            return;
        }
        
        String categoryName = req.getParameter("name");
        String categoryParam = req.getParameter("category");
        int id;
        if (categoryIds.containsKey(categoryName)) {
            id = categoryIds.get(categoryName);
        } else {
            // The following has the side effect of registering the newly deserialized Category in the Categories class.
            Category<?> category = gson.fromJson(categoryParam, Category.class);
            storage.registerCategory(category);

            id = currentCategoryId;
            categoryIds.put(categoryName, id);
            categories.put(id, category);
            currentCategoryId++;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(RESPONSE_JSON_CONTENT_TYPE);
        Writer writer = resp.getWriter();
        gson.toJson(id, writer);
        writer.flush();
    }

    @WebStoragePathHandler( path = "put-pojo" )
    private void putPojo(HttpServletRequest req, HttpServletResponse resp) {
        String insertParam = req.getParameter("insert");
        WebInsert insert = gson.fromJson(insertParam, WebInsert.class);
        int categoryId = insert.getCategoryId();
        Category<?> category = getCategoryFromId(categoryId);
        Put targetPut = null;
        if (insert.isReplace()) {
            if (! isAuthorized(req, resp, Roles.REPLACE)) {
                return;
            }
            targetPut = storage.createReplace(category);
        } else {
            if (! isAuthorized(req, resp, Roles.APPEND)) {
                return;
            }
            targetPut = storage.createAdd(category);
        }
        Class<? extends Pojo> pojoCls = category.getDataClass();
        String pojoParam = req.getParameter("pojo");
        Pojo pojo = gson.fromJson(pojoParam, pojoCls);
        targetPut.setPojo(pojo);
        targetPut.apply();
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "remove-pojo" )
    private void removePojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.DELETE)) {
            return;
        }
        
        String removeParam = req.getParameter("remove");
        WebRemove remove = gson.fromJson(removeParam, WebRemove.class);
        Remove targetRemove = storage.createRemove();
        targetRemove = targetRemove.from(getCategoryFromId(remove.getCategoryId()));
        Expression expr = remove.getWhereExpression();
        if (expr != null) {
            targetRemove.where(expr);
        }
        storage.removePojo(targetRemove);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @WebStoragePathHandler( path = "update-pojo" )
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void updatePojo(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.UPDATE)) {
            return;
        }
        
        try {
            String updateParam = req.getParameter("update");
            WebUpdate update = gson.fromJson(updateParam, WebUpdate.class);
            Update targetUpdate = storage.createUpdate(getCategoryFromId(update.getCategoryId()));
            Expression expr = update.getWhereExpression();
            if (expr != null) {
                targetUpdate.where(expr);
            }
            List<WebUpdate.UpdateValue> updates = update.getUpdates();
            if (updates != null) {
                String valuesParam = req.getParameter("values");
                JsonParser parser = new JsonParser();
                JsonArray jsonArray = parser.parse(valuesParam)
                        .getAsJsonArray();
                int index = 0;
                for (WebUpdate.UpdateValue updateValue : updates) {
                    Class valueClass = Class.forName(updateValue
                            .getValueClass());
                    Object value = gson.fromJson(jsonArray.get(index),
                            valueClass);
                    index++;
                    Key key = updateValue.getKey();
                    targetUpdate.set(key, value);
                }
            }
            targetUpdate.apply();
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    @WebStoragePathHandler( path = "query-execute" )
    private <T extends Pojo> void queryExecute(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.READ)) {
            return;
        }
        String queryParam = req.getParameter("prepared-stmt");
        WebPreparedStatement<T> stmt = gson.fromJson(queryParam, WebPreparedStatement.class);
        
        PreparedParameters p = stmt.getParams();
        PreparedStatementHolder<T> targetStmtHolder = getStatementHolderFromId(stmt.getStatementId());
        PreparedStatement<T> targetStmt = targetStmtHolder.getStmt();
        ParsedStatement<T> parsed = targetStmt.getParsedStatement();
        Query<T> targetQuery = null;
        ArrayList<T> resultList = new ArrayList<>();
        WebQueryResponse<T> response = new WebQueryResponse<>();
        try {
            targetQuery = (Query<T>)parsed.patchStatement(p.getParams());
            response.setResponseCode(WebQueryResponse.SUCCESS);
        } catch (IllegalPatchException e) {
            response.setResponseCode(WebQueryResponse.ILLEGAL_PATCH);
            writeResponse(resp, response, WebQueryResponse.class);
            return;
        }
        // TODO: Do proper query filtering
        targetQuery = fixQuery(targetQuery, stmt.getStatementId());
        Cursor<T> cursor = targetQuery.execute();
        while (cursor.hasNext()) {
            resultList.add(cursor.next());
        }
        T[] results = (T[])Array.newInstance(targetStmtHolder.getDataClass(), resultList.size());
        for (int i = 0; i < resultList.size(); i++) {
            results[i] = resultList.get(i);
        }
        response.setResultList(results);
        writeResponse(resp, response, WebQueryResponse.class);
    }

    @SuppressWarnings("rawtypes")
    private <T extends Pojo> Query fixQuery(Query<T> targetQuery, int statementId) {
        // TODO: Change the expression so as to perform proper filtering.
        return targetQuery;
    }

    private <T extends Pojo> PreparedStatementHolder<T> getStatementHolderFromId(int statementId) {
        @SuppressWarnings("unchecked") // we are the only ones adding them
        PreparedStatementHolder<T> holder = (PreparedStatementHolder<T>)preparedStatementIds.get(statementId);
        return holder;
    }

    private Category<?> getCategoryFromId(int categoryId) {
        Category<?> category = categories.get(categoryId);
        return category;
    }

    private void writeResponse(HttpServletResponse resp,
            Object responseObj, Class<?> typeOfResponseObj) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(RESPONSE_JSON_CONTENT_TYPE);
        gson.toJson(responseObj, typeOfResponseObj, resp.getWriter());
        resp.flushBuffer();
    }

    @WebStoragePathHandler( path = "generate-token" )
    private void generateToken(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (! isAuthorized(req, resp, Roles.CMD_CHANNEL_GENERATE) ) {
            return;
        }
        TokenManager tokenManager = (TokenManager) getServletContext().getAttribute(TOKEN_MANAGER_KEY);
        assert tokenManager != null;
        String clientTokenParam = req.getParameter("client-token");
        byte[] clientToken = Base64.decodeBase64(clientTokenParam);
        String actionName = req.getParameter("action-name");
        // Perform pre-authorization: Since it's the client user which issues
        // generate-token we have the correct user for which we can check role
        // membership - and, thus, determine if this action is allowed to be
        // performed. If the action is not allowed to be performed for this user
        // a 403 will get returned and further verify-token would also fail,
        // since no token gets added into the map. Trustworthiness of the
        // action name will be implicitly checked by verify-token.
        //
        // We authorize based on role membership of this user. I.e. in order
        // for a ping request (action-name == "ping") to properly authorize
        // the user needs to be a member of role 
        // "thermostat-cmdc-grant-ping". More generally, membership of role
        // "thermostat-cmdc-grant-<actionName>" grants the authenticated
        // user the <actionName> command channel action.
        String requiredRole = CMDC_AUTHORIZATION_GRANT_ROLE_PREFIX + actionName;
        if (! isAuthorized(req, resp, requiredRole)) {
            return;
        }
        // authorization succeeded at this point
        byte[] token = tokenManager.generateToken(clientToken, actionName);
        resp.setContentType("application/octet-stream");
        resp.setContentLength(token.length);
        resp.getOutputStream().write(token);
    }

    @WebStoragePathHandler( path = "verify-token" )
    private void verifyToken(HttpServletRequest req, HttpServletResponse resp) {
        if (! isAuthorized(req, resp, Roles.CMD_CHANNEL_VERIFY) ) {
            return;
        }
        TokenManager tokenManager = (TokenManager) getServletContext().getAttribute(TOKEN_MANAGER_KEY);
        assert tokenManager != null;
        String clientTokenParam = req.getParameter("client-token");
        byte[] clientToken = Base64.decodeBase64(clientTokenParam);
        String actionName = req.getParameter("action-name");
        byte[] token = Base64.decodeBase64(req.getParameter("token"));
        // Perform authentication of the request. We can't do authorization for
        // the originating client request here, since the only user info we have
        // in verify-token is the identity of the agent which the client asked
        // to perform the action for. Hence looking up role membership is not
        // what we want here in order to limit privileges of the client.
        //
        // Note that we achieve this by performing authorization checks during
        // generate-token. This is something the client user initiates and hence
        // there we have the required user information. The entire command
        // channel interaction can only succeed if and only if generate-token
        // AND verify-token succeeded for the same token. Thus it's OK to only
        // verify the token here - which in would only verify successfully if
        // generate-token worked properly as a first step.
        boolean verified = tokenManager.verifyToken(clientToken, token, actionName);
        if (! verified) {
            logger.log(Level.INFO, "Command channel action " + actionName + " from remote host " +
                                   req.getRemoteAddr() + " FAILED to authenticate!");
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        } else {
            logger.log(Level.FINEST, "Command channel action " + actionName + " from remote host " +
                    req.getRemoteAddr() + " PASSED authentication.");
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }
    
    private boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp, String role) {
        if (req.isUserInRole(role)) {
            return true;
        } else {
            logger.log(Level.INFO, "Not permitting access to " + req.getPathInfo() + ". User '" + req.getRemoteUser() + "' not in role " + role);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
    }


}

