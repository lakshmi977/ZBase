document.addEventListener("DOMContentLoaded", () => {
	// Add smooth scroll behavior for the header buttons
	document.querySelectorAll('button').forEach(button => {
		button.addEventListener('click', () => {
			if (button.textContent === 'Get Started') {
				window.scrollTo({
					top: document.querySelector('.features').offsetTop,
					behavior: 'smooth'
				});
			}
		});
	});

	// Add header background opacity based on scroll
	window.addEventListener('scroll', () => {
		const header = document.querySelector('header');
		const scrollPosition = window.scrollY;

		if (scrollPosition > 50) {
			header.style.background = 'rgba(15, 23, 42, 0.95)';
		} else {
			header.style.background = 'rgba(15, 23, 42, 0.8)';
		}
	});
});