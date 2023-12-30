package io.cahlee.blog.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "USERS")
@Getter
@Setter
public class User {
	
	@Id
	private String id;
	
	private String name;
	
	private String password;
	
	@Enumerated(EnumType.STRING)
	private UserRole userRole;
	
	@OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
	private List<Category> categories = new ArrayList<>();

	public static User map(UserDto userDto) {
		User user = new User();
		user.setId(userDto.getId());
		user.setName(userDto.getName());
		user.setPassword(userDto.getPassword());
		return user;
	}

}
