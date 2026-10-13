package dk.serik.recipes;

import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The tests' mail sender: it keeps every mail instead of sending it. A {@code @Component} in the
 * application's package, so the component scan of every {@code @SpringBootTest} picks it up. The
 * tests' {@code application.properties} sets no {@code spring.mail.host}, so Boot makes no sender
 * of its own, and this is the only one.
 */
@Component
public class RecordingMailSender implements MailSender {

	private final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();

	private MailException failure;

	@Override
	public void send(SimpleMailMessage... messages) {
		if (failure != null) {
			MailException thrown = failure;
			failure = null;
			throw thrown;
		}
		sent.addAll(Arrays.asList(messages));
	}

	/** Every mail kept so far to this address, oldest first. */
	public List<SimpleMailMessage> sentTo(String address) {
		return sent.stream().filter(message -> Arrays.asList(message.getTo()).contains(address)).toList();
	}

	/** The next send throws this, as a mail server that is down would; the one after succeeds. */
	public void failNextSend(MailException failure) {
		this.failure = failure;
	}
}
