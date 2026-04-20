import { json } from '@sveltejs/kit';
import nodemailer from 'nodemailer';
import {
	SMTP_HOST,
	SMTP_PORT,
	SMTP_USER,
	SMTP_PASS,
	RECEIPT_FROM
} from '$env/static/private';

export async function POST() {
	try {
		const transporter = nodemailer.createTransport({
			host: SMTP_HOST,
			port: Number(SMTP_PORT),
			secure: false, // IMPORTANT for port 587
			auth: {
				user: SMTP_USER,
				pass: SMTP_PASS
			}
		});

		await transporter.sendMail({
			from: RECEIPT_FROM,
			to: SMTP_USER, // sends to yourself for testing
			subject: 'Test Email',
			text: 'If you received this, your email setup works.',
			html: `<p>If you received this, your email setup works.</p>`
		});

		return json({ ok: true });
	} catch (err) {
		console.error(err);
		return json({ ok: false, error: 'Email failed' }, { status: 500 });
	}
}