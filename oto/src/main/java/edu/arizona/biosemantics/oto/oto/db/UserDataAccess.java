package edu.arizona.biosemantics.oto.oto.db;

/**
 * @author Partha Pratim Sanyal
 */
import java.io.File;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.apache.log4j.Logger;

import edu.arizona.biosemantics.oto.common.io.ExecCommmand;
import edu.arizona.biosemantics.oto.common.model.User;
import edu.arizona.biosemantics.oto.common.security.Encryptor;
import edu.arizona.biosemantics.oto.common.security.PasswordGenerator;
import edu.arizona.biosemantics.oto.common.security.SymmetricEncryption;
import edu.arizona.biosemantics.oto.oto.Configuration;
import edu.arizona.biosemantics.oto.oto.mail.NotifyEmail;

/**
 * This class will handle all user specific database queries
 * 
 * @author Partha
 * 
 */
public class UserDataAccess extends DatabaseAccess {

	private SymmetricEncryption symmetricEncryption = new SymmetricEncryption(Configuration.getInstance().getSecret());
	
	public UserDataAccess() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
		super();
	}

	private static final Logger LOGGER = Logger.getLogger(UserDataAccess.class);

	/**
	 * This method validates whether the user is registered in the database.
	 * This method has to be changed later in order to incorporate the logic of
	 * approved and disapproved users
	 * 
	 * @param user
	 * @return
	 * @throws SQLException
	 * @throws NoSuchAlgorithmException
	 */
	public boolean validateUser(User user) throws SQLException,
			NoSuchAlgorithmException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		ResultSet rset_role = null;
		String password = Encryptor.getInstance().encrypt(user.getPassword());
		String userEmail = user.getUserEmail();
		boolean returnFlag = false;
		
		try {
			conn = getConnection();
			pstmt = conn
					.prepareStatement("select * from users where email=? and password=? and status=? ");
			pstmt.setString(1, userEmail);
			pstmt.setString(2, password);
			pstmt.setString(3, "Y");
			rset = pstmt.executeQuery();

			if (rset.next()) {
				user.setPassword(password);
				user.setFirstName(rset.getString("firstname"));
				user.setLastName(rset.getString("lastname"));
				user.setAffiliation(rset.getString("affiliation"));
				user.setUserId(rset.getInt("userid"));
				user.setRole(rset.getString("role"));
				user.setBioportalUserId(rset.getString("bioportalUserId"));
				user.setBioportalApiKey(rset.getString("bioportalApiKey"));
				if (user.getRole().equals("U")) {
					// check if the user is a owner of any dataset
					pstmt = conn
							.prepareStatement("select * from dataset_owner where ownerID = "
									+ user.getUserId());
					rset_role = pstmt.executeQuery();
					if (rset_role.next()) {
						user.setRole("O");
					}
				}

				String status = rset.getString("status");
				if (status.equals("Y")) {
					user.setActive(true);
				}
				returnFlag = true;
			}

		} catch (SQLException exe) {
			LOGGER.error("Unable to validate user", exe);
			exe.printStackTrace();
		} finally {
			closeConnection(pstmt, rset, conn);
			if (rset_role != null) {
				rset_role.close();
			}
		}
		return returnFlag;
	}

	/**
	 * This function checks if the emailid is already taken
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean doesEmailIdExist(User user) throws Exception {
		boolean returnvalue = false;

		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;

		try {
			conn = getConnection();
			pstmt = conn
					.prepareStatement("select count(*) as count_ from users where email=?");
			pstmt.setString(1, user.getUserEmail().trim());
			rset = pstmt.executeQuery();

			if (rset.next()) {
				int count = rset.getInt("count_");
				if (count > 0) {
					returnvalue = true;
				}
			}
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("unable to check if the email exists");
			throw exe;
		} finally {
			closeConnection(pstmt, rset, conn);
		}

		return returnvalue;
	}

	public boolean updatePlainPassword() throws SQLException {
		boolean returnValue = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = getConnection();
			// get first name
			String sql = "select userid, password from users";
			pstmt = conn.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String password = rs.getString("password");
				int userid = rs.getInt("userid");

				if (password.length() < 20 || !password.endsWith("=")) {
					String encryptedPass = Encryptor.getInstance().encrypt(password);
					sql = "update users set password = ? where userid = ?";
					pstmt = conn.prepareStatement(sql);
					pstmt.setString(1, encryptedPass);
					pstmt.setInt(2, userid);
					pstmt.executeUpdate();
				}

			}

			returnValue = true;

		} catch (Exception exe) {
			LOGGER.error("unable to update the user resetPassword: ", exe);
			exe.printStackTrace();
		} finally {
			closeConnection(pstmt, conn);
			if (rs != null) {
				rs.close();
			}
		}
		return returnValue;
	}

	/**
	 * Reset user's password
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean resetPassword(String email) throws Exception {
		boolean returnValue = false;
		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		String password = new PasswordGenerator().generatePassword();
		String encryptedPass = Encryptor.getInstance().encrypt(password);
		String sql = "update users set password = ? where email = ?";
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, encryptedPass);
			pstmt.setString(2, email);
			pstmt.execute();

			// get first name
			sql = "select firstname from users where email = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				String fname = rs.getString("firstname");
				new NotifyEmail().sendPasswordInfo(email, fname, password);
			}

			returnValue = true;

		} catch (Exception exe) {
			LOGGER.error("unable to update the user resetPassword: ", exe);
			exe.printStackTrace();
			throw exe;
		} finally {
			closeConnection(pstmt, conn);
			if (rs != null) {
				rs.close();
			}

		}

		return returnValue;

	}

	/**
	 * This function is for registering users
	 * 
	 * @param user
	 * @param object 
	 * @return
	 * @throws Exception
	 */
	public boolean registerUser(User user) throws Exception {

		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "insert into users(email, openidprovider, password, firstname, lastname, affiliation) "
				+ "values (?,?,?,?,?,?)";
		boolean returnValue = false;
		String password = Encryptor.getInstance().encrypt(user.getPassword());
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, user.getUserEmail());
			pstmt.setString(2, user.getOpenIdProvider());
			pstmt.setString(3, password);
			pstmt.setString(4, user.getFirstName());
			pstmt.setString(5, user.getLastName());
			pstmt.setString(6, user.getAffiliation());
			pstmt.execute();
			returnValue = true;

			// delete existing file
			Configuration configuration = Configuration.getInstance();
			String userFilePath = configuration.getUserFilePath();
			File f = new File(userFilePath);
			if (f.exists()) {
				ExecCommmand ec = new ExecCommmand();
				ec.execShellCmd("rm " + userFilePath);
			}

			// generate user file
			try {
				sql = "select userid, email from users into outfile \""
						+ userFilePath + "\";";
				pstmt = conn.prepareStatement(sql);
				pstmt.execute();
			} catch(java.sql.SQLException e) {
				LOGGER.error("unable to write users to file. OS related due to failed rm command?", e);
			}
			
			new NotifyEmail().sendNewRegistrationNotification(
					user.getFirstName() + " " + user.getLastName(),
					user.getUserEmail());
		} catch (Exception exe) {
			LOGGER.error("unable to register user", exe);
			exe.printStackTrace();
			throw exe;
		} finally {
			closeConnection(pstmt, conn);
		}

		return returnValue;
	}

	/**
	 * This function gets all the currently registered users. For admin
	 * purposes.
	 * 
	 * @return
	 * @throws Exception
	 */
	public ArrayList<User> getAllUsers() {

		ArrayList<User> users = new ArrayList<User>();
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;

		try {
			conn = getConnection();
			pstmt = conn
					.prepareStatement("select userid, email, openidprovider, firstname, lastname, affiliation, status, role from users");
			rset = pstmt.executeQuery();
			while (rset.next()) {
				User user = new User();
				user.setUserId(rset.getInt("userid"));
				user.setOpenIdProvider(rset.getString("openidprovider"));
				user.setAffiliation(rset.getString("affiliation"));
				user.setFirstName(rset.getString("firstname"));
				user.setLastName(rset.getString("lastname"));
				user.setUserEmail(rset.getString("email"));
				user.setRole(rset.getString("role"));
				String status = rset.getString("status");
				if (status.equals("Y")) {
					user.setActive(true);
				} else {
					user.setActive(false);
				}
				users.add(user);
			}

		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("unable to get all users ", exe);
		} finally {
			try {
				closeConnection(pstmt, rset, conn);
			} catch (SQLException e) {
				LOGGER.error("unable to close connection ", e);
				e.printStackTrace();
			}
		}

		return users;
	}

	public User updateUserRole(User user) throws Exception {
		User rv = user;
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null, rset_role = null;
		try {
			conn = getConnection();
			pstmt = conn
					.prepareStatement("select role from users where userid = "
							+ user.getUserId());
			rset = pstmt.executeQuery();
			while (rset.next()) {
				rv.setRole(rset.getString("role"));

				// is owner of at least one dataset
				if (rv.getRole().equals("U")) {
					// check if the user is a owner of any dataset
					pstmt = conn
							.prepareStatement("select * from dataset_owner where ownerID = "
									+ user.getUserId());
					rset_role = pstmt.executeQuery();
					if (rset_role.next()) {
						rv.setRole("O");
					}
				}

				// has access to at least one finalized dataset
				if (rv.getRole().equals("U")) {
					pstmt = conn
							.prepareStatement("select * from datasetprefix where grouptermsdownloadable = true");
					rset_role = pstmt.executeQuery();
					if (rset_role.next()) {
						rv.setRole("F"); // finalized datasets
					}
				}
			}
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("unable to get user's role ", exe);
			throw exe;
		} finally {
			closeConnection(pstmt, rset, conn);
			if (rset_role != null) {
				rset_role.close();
			}
		}

		return rv;
	}

	/**
	 * This function updates the user's status
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean updateUserStatus(User user) throws Exception {
		boolean returnValue = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		String sql = "update users set status = ? where userid = ?";
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, user.isActive() ? "Y" : "N");
			pstmt.setInt(2, user.getUserId());
			pstmt.execute();
			returnValue = true;
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Unable to Update user status", exe);
			throw exe;
		} finally {
			closeConnection(pstmt, conn);
		}

		return returnValue;

	}

	/**
	 * This function updates the user's details
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public boolean updateUserDetails(User user) throws Exception {
		boolean returnValue = false;
		Connection conn = null;
		PreparedStatement pstmt = null;
		String password = Encryptor.getInstance().encrypt(user.getPassword());
		String sql = "update users set firstname = ?, lastname=?, affiliation=?, "
				+ "password = ?, email = ?, bioportalUserId = ?, "
				+ "bioportalApiKey = ? where userid = ?";
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, user.getFirstName());
			pstmt.setString(2, user.getLastName());
			pstmt.setString(3, user.getAffiliation());
			pstmt.setString(4, password);
			pstmt.setString(5, user.getUserEmail());
			pstmt.setString(6, user.getBioportalUserId());
			pstmt.setString(7, user.getBioportalApiKey());
			pstmt.setInt(8, user.getUserId());
			pstmt.execute();
			returnValue = true;

		} catch (Exception exe) {
			LOGGER.error("unable to update the user details: ", exe);
			exe.printStackTrace();
			throw exe;
		} finally {
			closeConnection(pstmt, conn);
		}

		return returnValue;

	}
	
	public User getUser(int userId) throws Exception {
		User user = new User();
		String sql = "select * FROM users WHERE userid = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, userId);
			pstmt.execute();
			ResultSet rset = pstmt.getResultSet();
			
			if(rset.next()) {
				user.setActive(rset.getString("status").equals("Y") ? true : false);
				user.setAffiliation(rset.getString("affiliation"));
				user.setBioportalApiKey(rset.getString("bioportalApiKey"));
				user.setBioportalUserId(rset.getString("bioportalUserId"));
				user.setFirstName(rset.getString("firstname"));
				user.setLastName(rset.getString("lastname"));
				user.setPassword(rset.getString("password"));
				user.setRole(rset.getString("role"));
				user.setUserEmail(rset.getString("email"));
				user.setUserId(rset.getInt("userid"));
				user.setOpenIdProvider(rset.getString("openidprovider"));
				return user;
			}
			throw new Exception("User does not exist");
			
		} catch (Exception exe) {
			LOGGER.error("unable to update the user details: ", exe);
			exe.printStackTrace();
			throw exe;
		} finally {
			closeConnection(pstmt, conn);
		}
	}
	
	public User getUser(String email) throws Exception {
		User user = new User();
		String sql = "select * FROM users WHERE email = ?";
		Connection conn = null;
		PreparedStatement pstmt = null;
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email);
			pstmt.execute();
			ResultSet rset = pstmt.getResultSet();
			
			if(rset.next()) {
				user.setActive(rset.getString("status").equals("Y") ? true : false);
				user.setAffiliation(rset.getString("affiliation"));
				user.setBioportalApiKey(rset.getString("bioportalApiKey"));
				user.setBioportalUserId(rset.getString("bioportalUserId"));
				user.setFirstName(rset.getString("firstname"));
				user.setLastName(rset.getString("lastname"));
				user.setPassword(rset.getString("password"));
				user.setRole(rset.getString("role"));
				user.setUserEmail(rset.getString("email"));
				user.setUserId(rset.getInt("userid"));
				user.setOpenIdProvider(rset.getString("openidprovider"));
				return user;
			}
			throw new Exception("User does not exist");
			
		} catch (Exception exe) {
			LOGGER.error("unable to update the user details: ", exe);
			exe.printStackTrace();
			throw exe;
		} finally {
			closeConnection(pstmt, conn);
		}
	}

	public boolean validateAuthentication(String authenticationToken) throws Exception {
		return this.getUserFromAuthenticationToken(authenticationToken) != null;		
	}
	
	public User getUserFromAuthenticationToken(String authenticationToken) throws Exception {
		byte[] bytes = DatatypeConverter.parseBase64Binary(authenticationToken);
		String plain = symmetricEncryption.decrypt(bytes);
		int userId = Integer.valueOf(plain.split(":")[1]);
		return this.getUser(userId);
	}

	public String getAuthenticationToken(User user) throws Exception {
		user = this.getUser(user.getUserEmail());
		byte[] bytes = symmetricEncryption.encrypt(Configuration.getInstance().getSecret() + ":" + user.getUserId());
		return DatatypeConverter.printBase64Binary(bytes);
	}
	

}
