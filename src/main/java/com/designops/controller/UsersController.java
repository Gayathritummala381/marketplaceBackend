
package com.designops.controller;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.designops.exception.UserAlreadyExistsException;
import com.designops.exception.UserNotFoundException;
import com.designops.model.Artifact;
import com.designops.model.ArtifactMenu;
import com.designops.model.Capability;
import com.designops.model.Component;
import com.designops.model.CustomTenant;
import com.designops.model.DesignFoundation;
import com.designops.model.Guide;
import com.designops.model.LibraryForm;
import com.designops.model.LoginDetail;
//import com.designops.model.LoginHistory;
import com.designops.model.Tenant;
import com.designops.model.TenantUser;
import com.designops.model.Users;
import com.designops.repository.ArtifactRepository;
//import com.designops.repository.LoginHistoryRepository;
import com.designops.repository.RoleRepository;
import com.designops.repository.TenantRepository;
import com.designops.repository.TenantUserRepository;
import com.designops.repository.UsersRepository;
import com.designops.utility.Constant;
import com.designops.utility.Email;
import com.mongodb.client.result.UpdateResult;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class UsersController {

  @Autowired
  private UsersRepository usersRepository;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private RoleRepository roleRepository;

  @Autowired
  private TenantUserRepository tenantUserRepository;

  @Autowired
  private TenantRepository tenantRepository;

  @Autowired
  ArtifactRepository artifactRepository;

  @Autowired
  MongoTemplate mongoTemplate;

  @GetMapping("/")
  public void getEmpty(HttpServletResponse response) throws IOException {
    response.sendRedirect(Constant.rootPath);
  }

  public static boolean checkPassword(String inputPassword, String encryptedStoredPassword) {
    StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();
    boolean result = encryptor.checkPassword(inputPassword, encryptedStoredPassword);
    return encryptor.checkPassword(inputPassword, encryptedStoredPassword);
  }

  public List < CustomTenant > returnTenantListConainsCategory(List < CustomTenant > customTenantList, String category) {
    List < CustomTenant > customTenantListContainingCategory = new ArrayList < > ();
    for (int i = 0; i < customTenantList.size(); i++) {
      CustomTenant customTenant = customTenantList.get(i);
      List < String > customTenantArtifactCategoryList = Arrays.asList(customTenant.getArtifactCategory().split(","));
      if (customTenantArtifactCategoryList.contains(category)) {
        customTenantListContainingCategory.add(customTenant);
      }
    }
    return customTenantListContainingCategory;
  }

  @GetMapping("/tenantsByAdmin/{id}")
  public ResponseEntity < Users > getTenantsForUserWithAdminRole(@PathVariable(value = "id") Integer userId) throws UserNotFoundException {
    Users foundUsers = usersRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found for this id :: + userId"));

    if (foundUsers.getIsActive().equals("1")) {
      List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(foundUsers.getUserId());
      List < TenantUser > activeTenantUserList = new ArrayList < > ();
      for (int j = 0; j < tenantUserList.size(); j++) {
        TenantUser tenantUser = tenantUserList.get(j);
        if (!(foundUsers.getIsSuperAdmin().equalsIgnoreCase("1"))) {
          if (tenantUser.getIsActive().equals("1") && (tenantUser.getRoleName().equalsIgnoreCase("Admin"))) {
            activeTenantUserList.add(tenantUser);
          }
        } else {
          if (tenantUser.getIsActive().equals("1")) {
            activeTenantUserList.add(tenantUser);
          }
        }
      }
      if (!(activeTenantUserList.isEmpty())) {
        foundUsers.setTenantUser(activeTenantUserList);
      }
      return new ResponseEntity < Users > (foundUsers, HttpStatus.OK);
    } else

    {
      throw new UserNotFoundException("User not found for this id :: " + userId);
    }

  }

  @GetMapping("/user")
  public ResponseEntity < List < Users >> getallUsers() throws UserNotFoundException {
    List < Users > usersList = usersRepository.findAll();
    List < Users > activeUserList = new ArrayList < > ();
    for (int i = 0; i < usersList.size(); i++) {
      Users users = usersList.get(i);
      if (users.getIsActive().equals("1")) {
        List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(users.getUserId());
        List < TenantUser > activeTenantUserList = new ArrayList < > ();
        for (int j = 0; j < tenantUserList.size(); j++) {
          TenantUser tenantUser = tenantUserList.get(j);
          if (tenantUser.getIsActive().equals("1")) {
            Tenant foundTenant = tenantRepository.findByTenantId(tenantUser.getTenantId()).get(0);
            tenantUser.setTenantName(foundTenant.getTenantName());
            activeTenantUserList.add(tenantUser);
          }
          }
          if (!(activeTenantUserList.isEmpty())) {
            users.setTenantUser(activeTenantUserList);
          }
          activeUserList.add(users);
        }
      }
      if (usersList.isEmpty() || activeUserList.isEmpty()) {
        throw new UserNotFoundException("There are no Users present");
      }
    
    return new ResponseEntity < List < Users >> (activeUserList, HttpStatus.OK);

  }

  @PostMapping("/user")
  @CrossOrigin(origins = "http://localhost:4209")
  public ResponseEntity < Users > adduser(@Valid @RequestBody Users users) throws AddressException, MessagingException {
    Users foundUser = usersRepository.findByEmailIgnoreCase(users.getEmail());
    if (foundUser == null || foundUser.getIsActive().equalsIgnoreCase("")) {
      int roleId = roleRepository.findByRoleName(Constant.defaultRoLeName).getRoleId();
      Users user = new Users();
      TenantUser tenantUser;
      user.setEmail(users.getEmail());
      user.setUserName(users.getEmail());
      user.setName(users.getName());
      user.setIsActive("1");
      user.setIsSuperAdmin("");
      StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();
      user.setPassword(encryptor.encryptPassword(Constant.randomPassword));
      Users insertedUser = usersRepository.save(user);
      if (!(users.getTenantUser().isEmpty())) {
        for (int i = 0; i < users.getTenantUser().size(); i++) {
          tenantUser = new TenantUser();
          tenantUser.setTenantId(users.getTenantUser().get(i).getTenantId());
          tenantUser.setUserId(user.getUserId());
          tenantUser.setRoleId(roleId);
          tenantUser.setRoleName(Constant.defaultRoLeName);
          tenantUser.setEmail(users.getEmail());
          tenantUser.setName(users.getName());
          tenantUser.setIsActive("1");
          tenantUserRepository.save(tenantUser);
        }
      }
      Email.sendEmail(Constant.fromMail, users.getEmail(), Constant.host, Constant.randomPassword);
      return new ResponseEntity < Users > (insertedUser, HttpStatus.CREATED);
    } else {
      throw new UserAlreadyExistsException("User already exists with this email ::" + users.getEmail());
    }
  }

  @PostMapping("/addTenantToUser")
  public ResponseEntity < TenantUser > addTenantToUser(@RequestBody TenantUser tenantUser) {
    Tenant existingTenant = tenantRepository.findByTenantId(tenantUser.getTenantId()).get(0);
    if ((existingTenant != null) && (existingTenant.getIsActive().equals("1"))) {
      tenantUser.setTenantId(existingTenant.getTenantId());
      return new ResponseEntity < TenantUser > (tenantUser, HttpStatus.OK);
    } else {
      throw new UserNotFoundException("Tenant not found");
    }
  }

  @GetMapping("/approvalQueue/{id}")
  public List < List < Artifact >> getApprovalQueue(@PathVariable(value = "id") Integer userId) {
    ResponseEntity < Users > responseEntity = restTemplate
      .getForEntity(Constant.getTenantsForUserWithAdminRoleURL + userId, Users.class);
    Users users = responseEntity.getBody();
    List < List < Artifact >> artifactList = new ArrayList < > ();
    List < TenantUser > tenantUserList = users.getTenantUser();
    List < TenantUser > activeTenantUserList = new ArrayList < > ();
    for (int i = 0; i < tenantUserList.size(); i++) {
      TenantUser tenantUser = tenantUserList.get(i);
      if (tenantUser.getIsActive().equals("1")) {
        activeTenantUserList.add(tenantUser);
        System.out.println("Inside Active Tenant User*** tenant id active=" + tenantUser.getTenantId());
      }
    }
    if (!(activeTenantUserList.isEmpty())) {
      System.out.println(" Active Tenant User list is not empty");
      for (int i = 0; i < activeTenantUserList.size(); i++) {
        int tenantId = activeTenantUserList.get(i).getTenantId();
        System.out.println("Tenant ID Found=" + tenantId);
        String url = Constant.getALlArifactsByTenantIdURL + tenantId;
        ResponseEntity < Artifact[] > artifactResponseEntity = restTemplate.getForEntity(url, Artifact[].class);
        if (artifactResponseEntity.hasBody()) {
          List < Artifact > foundArtifactList = Arrays.asList(artifactResponseEntity.getBody());
          if (!(foundArtifactList.isEmpty())) {

            List < Artifact > activeArtifactList = new ArrayList < > ();
            for (int k = 0; k < foundArtifactList.size(); k++) {
              Artifact artifact = foundArtifactList.get(k);
              if (artifact.isActive()) {
                activeArtifactList.add(artifact);
              }
            }
            // Collections.sort(activeArtifactList, Comparator.com);
            // Collections.sort(activeArtifactList, Artifact al, Artifact a2) ->
            //al..compareTo(a.name));
            // Collections.sort(activeArtifactList, Comparator.comparingInt(Artifact
            // ::getLast Modifiedon ());
            Collections.sort(activeArtifactList, new Comparator < Artifact > () {
              public int compare(Artifact a1, Artifact a2) {
                return a2.getLastModifiedOn().compareTo(a1.getLastModifiedOn());
              }
            });
            artifactList.add(activeArtifactList);
          }
        }
      }
    }
    return artifactList;
  }

  @PostMapping("/login")
  public ResponseEntity < List < ArtifactMenu >> executeLogin(@RequestBody LoginDetail loginDetail, HttpServletRequest request) {
    Users user = usersRepository.findByEmailIgnoreCase(loginDetail.getEmail());
    System.out.println("Username=" + user.getEmail());
    if (Objects.nonNull(user) && user.getIsActive().equals("1")) {
      //checkPassword(loginDetail.getPassword(), user.getPassword())
      if (checkPassword(loginDetail.getPassword(), user.getPassword())) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:55");
        LocalDateTime currentDate = LocalDateTime.now();
        //        	   		LoginHistory loginHistory= new LoginHistory();
        //        	   		loginHistory.setEmail(loginDetail.getEmail();
        //        	   		loginHistory.setUsername (user.getName();
        //        	   		loginHistory.setTimestamp (currentDate);
        //        	   		loginHistoryRepository.save(loginHistory);
        List < String > artifactsList = Arrays.asList("Design Foundations", "Components", "Capabilities",
          "Libraries", "Guides");
        ArtifactMenu artifactMenu;

        List < ArtifactMenu > artifactMenulist = new ArrayList < > ();
        List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(user.getUserId());
        List < TenantUser > activeTenantUserList = new ArrayList < > ();
        for (int i = 0; i < tenantUserList.size(); i++) {
          TenantUser tenantUser = tenantUserList.get(i);
          if (tenantUser.getIsActive().equals("1")) {
            activeTenantUserList.add(tenantUser);
          }
        }
        List < CustomTenant > customTenantList = new ArrayList < > ();
        if (!(activeTenantUserList.isEmpty())) {
          CustomTenant customTenant;
          for (int i = 0; i < activeTenantUserList.size(); i++) {
            customTenant = new CustomTenant();
            TenantUser currentTenantUser = activeTenantUserList.get(i);
            Tenant tenant = tenantRepository.findByTenantId(currentTenantUser.getTenantId()).get(0);
            //	Tenant tenant = tenantRepository.findByTenantId(currentTenantUser.getTenantId())
            customTenant.setTenantId(tenant.getTenantId());
            customTenant.setTenantName(tenant.getTenantName());
            customTenant.setArtifactCategory(tenant.getArtifactCategory());
            customTenant.setRoleId(currentTenantUser.getRoleId());
            customTenant.setRoleName(currentTenantUser.getRoleName());
            customTenantList.add(customTenant);
          }
        }
        for (int i = 0; i < artifactsList.size(); i++) {
          artifactMenu = new ArtifactMenu();
          artifactMenu.setArtifactName(artifactsList.get(i));
          artifactMenu.setIsSuperAdmin(user.getIsSuperAdmin());
          artifactMenu.setName(user.getName());
          artifactMenu.setUserId(user.getUserId());
          if (!(customTenantList.isEmpty())) {
            artifactMenu.setCustomTenantList(returnTenantListConainsCategory(customTenantList, artifactsList.get(i)));
            //	artifactMenu.setCustomTenantList(returnTenantListConainsCategory(customTenantList, artifactsList.get(i)));
          }
          artifactMenulist.add(artifactMenu);
        }
        //return new ResponseEntity<List<ArtifactMenu>>(artifactMenulist, HttpStatus.OK);
        return new ResponseEntity < List < ArtifactMenu >> (artifactMenulist, HttpStatus.OK);

      } else {

        return new ResponseEntity < List < ArtifactMenu >> (HttpStatus.UNAUTHORIZED);
      }
    } else {
      return new ResponseEntity < List < ArtifactMenu >> (HttpStatus.UNAUTHORIZED);
    }
  }

  @GetMapping("/pageManagement/{id}")
  @CrossOrigin
  public ResponseEntity < List < ArtifactMenu >> getPageManagement(@PathVariable(value = "id") Integer userId) {
    Users user = usersRepository.findByUserId(userId).get(0);
    if (Objects.nonNull(user) && user.getIsActive().equals("1")) {
      List < String > artifactsList = Arrays.asList("Design Foundations", "Components", "Capabilities", "Libraries",
        "Guides");
      ArtifactMenu artifactMenu;
      List < ArtifactMenu > artifactMenuList = new ArrayList < > ();
      List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(user.getUserId());
      List < TenantUser > activeTenantUserList = new ArrayList < > ();
      for (int i = 0; i < tenantUserList.size(); i++) {
        TenantUser tenantUser = tenantUserList.get(i);
        if (tenantUser.getIsActive().equals("1")) {
          if (tenantUser.getRoleName().equalsIgnoreCase("Admin")) {
            activeTenantUserList.add(tenantUser);
          }
        }
      }
      List < CustomTenant > customTenantlist = new ArrayList < > ();
      if (!(activeTenantUserList.isEmpty())) {
        CustomTenant customTenant;
        for (int i = 0; i < activeTenantUserList.size(); i++) {
          customTenant = new CustomTenant();
          TenantUser currentTenantUser = activeTenantUserList.get(i);
          Tenant tenant = tenantRepository.findByTenantId(currentTenantUser.getTenantId()).get(0);
          customTenant.setTenantId(tenant.getTenantId());
          customTenant.setTenantName(tenant.getTenantName());
          customTenant.setArtifactCategory(tenant.getArtifactCategory());
          customTenant.setRoleId(currentTenantUser.getRoleId());
          customTenant.setRoleName(currentTenantUser.getRoleName());
          customTenantlist.add(customTenant);
        }
      }
      for (int i = 0; i < artifactsList.size(); i++) {
        artifactMenu = new ArtifactMenu();
        artifactMenu.setArtifactName(artifactsList.get(i));
        artifactMenu.setIsSuperAdmin(user.getIsSuperAdmin());
        artifactMenu.setName(user.getName());
        artifactMenu.setUserId(user.getUserId());
        if (!(customTenantlist.isEmpty())) {
          artifactMenu.setCustomTenantList(returnTenantListConainsCategory(customTenantlist, artifactsList.get(i)));
        }
        artifactMenuList.add(artifactMenu);
      }
      return new ResponseEntity < List < ArtifactMenu >> (artifactMenuList, HttpStatus.OK);
    } else {
      return new ResponseEntity < List < ArtifactMenu >> (HttpStatus.NO_CONTENT);
    }
  }

  @PostMapping("/tenantsByArtifacts/{email]")
  public ResponseEntity < List < ArtifactMenu >> getPageForAdmin(@PathVariable(value = "email") String email) {
    Users user = usersRepository.findByEmailIgnoreCase(email);
    if (Objects.nonNull(user) && user.getIsActive().equals("1")) {
      List < String > artifactsList = Arrays.asList("Design Foundations", "Components", "Capabilities", "Libraries",
        "Guides");
      ArtifactMenu artifactMenu;
      List < ArtifactMenu > artifactMenuList = new ArrayList < > ();
      List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(user.getUserId());
      List < TenantUser > activeTenantUserList = new ArrayList < > ();
      for (int i = 0; i < tenantUserList.size(); i++) {
        TenantUser tenantUser = tenantUserList.get(i);
        if (tenantUser.getIsActive().equals("1")) {
          activeTenantUserList.add(tenantUser);
        }
      }
      List < CustomTenant > customTenantList = new ArrayList < > ();
      if (!(activeTenantUserList.isEmpty())) {
        CustomTenant customTenant;
        for (int i = 0; i < activeTenantUserList.size(); i++) {
          customTenant = new CustomTenant();
          TenantUser currentTenantUser = activeTenantUserList.get(i);
          if (currentTenantUser.getRoleName().equalsIgnoreCase("Admin")) {
            Tenant tenant = tenantRepository.findByTenantId(currentTenantUser.getTenantId()).get(0);
            customTenant.setTenantId(tenant.getTenantId());
            customTenant.setTenantName(tenant.getTenantName());
            customTenant.setArtifactCategory(tenant.getArtifactCategory());
            customTenant.setRoleId(currentTenantUser.getRoleId());
            customTenant.setRoleName(currentTenantUser.getRoleName());
            customTenantList.add(customTenant);
          }
        }
      }
      for (int i = 0; i < artifactsList.size(); i++) {
        artifactMenu = new ArtifactMenu();
        artifactMenu.setArtifactName(artifactsList.get(i));
        artifactMenu.setIsSuperAdmin(user.getIsSuperAdmin());
        artifactMenu.setName(user.getName());
        artifactMenu.setUserId(user.getUserId());
        if (!(customTenantList.isEmpty())) {
          artifactMenu.setCustomTenantList(
            returnTenantListConainsCategory(customTenantList, artifactsList.get(i)));
        }
        artifactMenuList.add(artifactMenu);
      }
      return new ResponseEntity < List < ArtifactMenu >> (artifactMenuList, HttpStatus.OK);
    } else {
      return new ResponseEntity < List < ArtifactMenu >> (HttpStatus.NO_CONTENT);
    }
  }

  @GetMapping("/user/{id}")
  public ResponseEntity < Users > getUserByID(@PathVariable(value = "id") Integer userId) throws UserNotFoundException {
    Users foundUsers = usersRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found for this id :: + userId"));

    if (foundUsers.getIsActive().equals("1")) {
      List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(foundUsers.getUserId());
      List < TenantUser > activeTenantUserList = new ArrayList < > ();
      for (int j = 0; j < tenantUserList.size(); j++) {
        TenantUser tenantUser = tenantUserList.get(j);
        if (tenantUser.getIsActive().equals("1")) {
          Tenant foundTenant = tenantRepository.findByTenantId(tenantUser.getTenantId()).get(0);
          tenantUser.setTenantName(foundTenant.getTenantName());
          activeTenantUserList.add(tenantUser);
        }
      }
      if (!(activeTenantUserList.isEmpty())) {
        foundUsers.setTenantUser(activeTenantUserList);
      }
      return new ResponseEntity < Users > (foundUsers, HttpStatus.OK);
    } else {
      throw new UserNotFoundException("User not found for this id :: + userId");
    }
  }

  public TenantUser istenantPresent(int newTenantId, List < TenantUser > existingTenantUserList) {
    TenantUser existingTenantUser = null;
    for (int i = 0; i < existingTenantUserList.size(); i++) {
      int existingTenantId = existingTenantUserList.get(i).getTenantId();
      if (newTenantId == existingTenantId) {
        existingTenantUser = existingTenantUserList.get(i);
      }
    }
    return existingTenantUser;

  }

  @PutMapping("/user/{id}")
  public ResponseEntity < Users > updateUser(@Valid @PathVariable("id") int userId, @RequestBody Users users)
  throws UserNotFoundException {
    List < Users > existingUserList = usersRepository.findByUserId(userId);
    if (!(existingUserList.isEmpty())) {
      Users existingusers = existingUserList.get(0);
      if (!(existingusers.getEmail().equalsIgnoreCase(users.getEmail()))) {
        Users userFoundByEmail = usersRepository.findByEmailIgnoreCase(users.getEmail());
        if (userFoundByEmail == null) {
          existingusers.setEmail(users.getEmail());
        } else {
          throw new UserAlreadyExistsException("User already exists with this Email id:" + users.getEmail());
        }
      }
      existingusers.setName(users.getName());
      existingusers.setIsActive("1");
      existingusers.setUserId(userId);
      existingusers.setUserName(users.getEmail());
      if (existingusers.getIsSuperAdmin().equalsIgnoreCase("")) {
        existingusers.setIsSuperAdmin("");
      }
      Users updatedUser = usersRepository.save(existingusers);
      List < TenantUser > existingTenantUserList = tenantUserRepository.findByUserId(userId);
      List < TenantUser > newTenantUserList = users.getTenantUser();
      for (int i = 0; i < existingTenantUserList.size(); i++) {
        TenantUser foundTenantUser = existingTenantUserList.get(i);
        foundTenantUser.setIsActive("");
        tenantUserRepository.save(foundTenantUser);
      }

      for (int i = 0; i < newTenantUserList.size(); i++) {
        TenantUser newTenantUser = newTenantUserList.get(i);
        int newTenantId = newTenantUser.getTenantId();
        TenantUser existingTenantUser = istenantPresent(newTenantId, existingTenantUserList);
        if (existingTenantUser != null) {
          // update existing Tenant User
          existingTenantUser.setTenantId(newTenantUser.getTenantId());
          existingTenantUser.setUserId(userId);
          existingTenantUser.setRoleId(newTenantUser.getRoleId());
          existingTenantUser.setRoleName(newTenantUser.getRoleName());
          existingTenantUser.setEmail(users.getEmail());
          existingTenantUser.setName(users.getName());
          existingTenantUser.setIsActive("1");
          tenantUserRepository.save(existingTenantUser);
        } else {
          // create new tenant user
          TenantUser addNewTenantUser = new TenantUser();
          addNewTenantUser.setTenantId(newTenantUser.getTenantId());
          addNewTenantUser.setUserId(userId);
          addNewTenantUser.setRoleId(newTenantUser.getRoleId());
          addNewTenantUser.setRoleName(newTenantUser.getRoleName());
          addNewTenantUser.setEmail(users.getEmail());
          addNewTenantUser.setName(users.getName());
          addNewTenantUser.setIsActive("1");
          tenantUserRepository.save(addNewTenantUser);
        }
      }
      List < Artifact > artifactList = artifactRepository.findbyUserId(updatedUser.getUserId());

      if (!artifactList.isEmpty()) {
        for (int i = 0; i < artifactList.size(); i++) {
          String artifactCategory = artifactList.get(i).getArtifactCategory();
          int artifactId = artifactList.get(i).getArtifactId();
          Artifact artifacts = artifactList.get(i);
          artifacts.setCreatedBy(updatedUser.getName());
          artifactRepository.save(artifacts);
          if (artifactCategory.equalsIgnoreCase("Capabilities")) {
            Query select = Query.query(Criteria.where("artifactId").is(artifactId));
            Update update = new Update();
            update.set("userName", updatedUser.getName());
            UpdateResult updateResult = mongoTemplate.updateMulti(select, update, Capability.class);
          }
          if (artifactCategory.equalsIgnoreCase("Components")) {
            Query select = Query.query(Criteria.where("artifactId").is(artifactId));
            Update update = new Update();
            update.set("userName", updatedUser.getName());
            UpdateResult updateResult = mongoTemplate.updateMulti(select, update, Component.class);

          }
          if (artifactCategory.equalsIgnoreCase("Design Foundations")) {
            Query select = Query.query(Criteria.where("artifactId").is(artifactId));
            Update update = new Update();
            update.set("userName", updatedUser.getName());
            UpdateResult updateResult = mongoTemplate.updateMulti(select, update, DesignFoundation.class);
          }
          if (artifactCategory.equalsIgnoreCase("Guides")) {
            Query select = Query.query(Criteria.where("artifactId").is(artifactId));
            Update update = new Update();
            update.set("userName", updatedUser.getName());
            UpdateResult updateResult = mongoTemplate.updateMulti(select, update, Guide.class);
          }
          if (artifactCategory.equalsIgnoreCase("Libraries")) {
            Query select = Query.query(Criteria.where("artifactId").is(artifactId));
            Update update = new Update();
            update.set("userName", updatedUser.getName());
            UpdateResult updateResult = mongoTemplate.updateMulti(select, update, LibraryForm.class);
          }
        }
      }

      return new ResponseEntity < Users > (updatedUser, HttpStatus.OK);
    } else {
      throw new UserNotFoundException("User not found");
    }
  }

  @PostMapping("/forgotpassword")
  public String forgotPassword(@RequestBody LoginDetail loginDetail) throws UserNotFoundException {

    Users presentUser = usersRepository.findByEmailIgnoreCase(loginDetail.getEmail());
    if (presentUser != null) {
      StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();
      System.out.println("Forgot Passwod OTP:" + Constant.password);
      presentUser.setPassword(encryptor.encryptPassword(Constant.password));
      Email.sendEmail(Constant.fromMail, loginDetail.getEmail(), Constant.host, Constant.randomPassword);
      usersRepository.save(presentUser);
      return "One Time Password is sent to registered email id, use this and generate new password.";
    } else {
      return "User not found with this email id, Please enter registered email id";
    }
  }

  @PostMapping("/changepassword")
  public String changePassword(@RequestBody LoginDetail loginDetail) throws UserNotFoundException {
    Users presentUser = usersRepository.findByEmailIgnoreCase(loginDetail.getEmail());
    StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();
    if (presentUser != null) {
      if (checkPassword(loginDetail.getPassword(), presentUser.getPassword())) {
        StrongPasswordEncryptor encrypt = new StrongPasswordEncryptor();
        presentUser.setPassword(encrypt.encryptPassword(loginDetail.getNewPassword()));
        usersRepository.save(presentUser);
        return "Password Reset Successfully !";
      } {
        return "One Time Password did not match ";
      }
    } else {
      return "User not found with this email id, Please enter registered email id";
    }
  }

  @PostMapping("/deleteuser")
  public ResponseEntity < Map < String, Boolean >> deleteMultipleUser(@RequestBody List < Integer > userIds)
  throws UserNotFoundException {
    Map < String, Boolean > response = new HashMap < > ();
    response.put("deleted", Boolean.TRUE);
    for (int k = 0; k < userIds.size(); k++) {
      int userId = userIds.get(k);
      Users users = usersRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found for this id ::" + userId));
      List < TenantUser > tenantUserList = tenantUserRepository.findByUserId(userId);
      if (tenantUserList.isEmpty()) {
        System.out.println("tenant user list is empty & user name=" + users.getName());
        users.setIsActive("");
        usersRepository.save(users);
        return new ResponseEntity < Map < String, Boolean >> (response, HttpStatus.ACCEPTED);
      } else {
        System.out.println("tenant user list is not empty & user name=" + users.getName());
        users.setIsActive("");
        for (int i = 0; i < tenantUserList.size(); i++) {
          tenantUserList.get(i).setIsActive("");
        }
        usersRepository.save(users);
      }

    }
    return new ResponseEntity < Map < String, Boolean >> (response, HttpStatus.ACCEPTED);
  }

  @GetMapping("/users/{userName}")
  public ResponseEntity < List < Users >> getUserByName(@PathVariable String userName) throws UserNotFoundException {
    List < Users > usersList = usersRepository.findByNameIgnoreCase(userName);
    if (!usersList.isEmpty()) {
      return new ResponseEntity < List < Users >> (usersList, HttpStatus.OK);
    } else {
      return new ResponseEntity < List < Users >> (usersList, HttpStatus.NO_CONTENT);
    }
  }

  public boolean istenantConainsCategory(Tenant tenant, String category) {
    List < String > tenantArtifactCategoryList = Arrays.asList(tenant.getArtifactCategory().split(","));
    return tenantArtifactCategoryList.contains(category);
  }

  public boolean isTenantConainsCategory(Tenant tenant, String category) {
    List < String > tenantArtifactCategoryList = Arrays.asList(tenant.getArtifactCategory().split(","));
    return tenantArtifactCategoryList.contains(category);
  }

}
