package dk.serik.recipes.bean;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@RequestScope
@Component
@Data
public class Session {
	
	private String userName;
	
	private String sessionToken;
}
