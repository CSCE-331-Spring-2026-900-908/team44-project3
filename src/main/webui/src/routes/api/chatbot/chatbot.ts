import { json, type RequestEvent } from '@sveltejs/kit';
import { GoogleGenAI } from '@google/genai';
import { env } from '$env/dynamic/private';
import fs from 'fs';
import path from 'path';

const MODEL_NAME = 'gemini-2.5-flash-preview-05-14';

export async function POST({ request }: RequestEvent) {
    const { prompt } = await request.json();

    if (!prompt || typeof prompt !== 'string') {
        return json({ text: 'Invalid input.' }, { status: 400 });
    }

    const apiKey = env.GEMINI_API_KEY;
    if (!apiKey) {
        return json({ text: 'Boba Bob is currently offline! (Missing API Key)' }, { status: 500 });
    }

    const ai = new GoogleGenAI({ apiKey });

    const filePath = path.resolve(process.cwd(), 'src/lib/api/instructions.md');
    let systemInstructionText = "You are a friendly boba shop assistant named Boba Bob.";

    try {
        systemInstructionText = fs.readFileSync(filePath, 'utf8');
    } catch (error) {
        console.error('[chatbot] failed to load instructions file:', error);
    }

    try {
        const response = await ai.models.generateContent({
            model: MODEL_NAME,
            contents: [{
                role: 'user',
                parts: [{ text: prompt }]
            }],
            config: {
                systemInstruction: systemInstructionText,
                thinkingConfig: {
                    thinkingBudget: 0
                }
            }
        });

        const botText = response.text ?? "I'm not sure how to answer that!";
        return json({ text: botText });

    } catch (error: any) {
        console.error("Gemini API Error:", error);
        return json({
            text: "Boba Bob is currently offline!",
            detail: error.message
        }, { status: 500 });
    }
}