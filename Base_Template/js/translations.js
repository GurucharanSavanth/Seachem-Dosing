// js/translations.js v5.0

const translations = {
    en: {
        main_title: "Aquarium Calculator",
        subtitle: "Dosing & Water Parameter Analysis",
        param_status_title: "Water Parameter Status",
        core_params_title: "Core Parameters",
        param_ammonia_label: "Ammonia (NH₃/NH₄⁺)",
        param_nitrate_label: "Nitrate (NO₃)",
        param_nitrite_label: "Nitrite (NO₂)",
        param_gh_label: "General Hardness (GH)",
        param_kh_label: "Carbonate Hardness (KH)",
        emergency_title: "Emergency Recommendations",
        reco_ok: "No issues detected. All parameters look good!",
        reco_ammonia_detected: "Ammonia detected!",
        reco_prime_dose: "Dose <strong>{primeDose} mL of Seachem Prime</strong> immediately to detoxify.",
        reco_nitrite_detected: "Nitrite detected!",
        reco_stability_dose: "Add <strong>{stabilityDose} mL of Seachem Stability</strong> to boost beneficial bacteria.",
        reco_gh_low: "GH is very low. Consider adding <strong>Seachem Equilibrium</strong> to raise it.",
        reco_kh_low: "KH is very low, which can cause pH swings. Add a <strong>KH booster</strong>.",
        reco_nitrate_high: "High Nitrates (>50ppm). Consider a <strong>30-50% water change</strong>.",
        reco_volume_needed: "Enter water volume to calculate emergency doses.",
        status_good: "Good",
        status_high: "High",
        status_danger: "DANGER",
        dosing_calculators_title: "Dosing Calculators",
        water_volume_title: "1. Water Volume",
        net_volume_label: "Net Water Volume",
        unit_label: "Unit",
        kh_booster_title: "2. KH Booster (Potassium Bicarbonate)",
        gh_booster_title: "3. GH Booster (Seachem Equilibrium)",
        safe_title: "4. Water Conditioner (Seachem Safe)",
        apt_title: "5. Plant Fertilizer (APT Complete)",
        ph_neutralizer_title: "6. pH Neutralizer (Seachem Neutral Regulator)",
        kh_reducer_title: "7. KH & pH Reducer (Seachem Acid Buffer)",
        goldfish_buffer_title: "8. Goldfish Buffer (Seachem Gold Buffer)",
        current_kh_label: "Current KH (°dKH)",
        target_kh_label: "Target KH (°dKH)",
        purity_label: "Purity (0.5-1.0)",
        current_gh_label: "Current GH (°dGH)",
        target_gh_label: "Target GH (°dGH)",
        current_ph_label: "Current pH",
        target_ph_label: "Target pH",
        no_dose_needed: "No dose needed",
        changelog_btn: "v5.0 Changelog",
        calculate_btn: "Calculate Doses",
        download_csv_btn: "Download CSV",
        reset_btn: "Reset All",
        changelog_title: "Version 5.0 Changelog",
        changelog_list: `
            <li><strong>New:</strong> Stoichiometric KHCO3 calculation using molecular weight (100.115 g/mol)</li>
            <li><strong>New:</strong> LxBxH tank dimension calculator with cm/in/ft units</li>
            <li><strong>New:</strong> Seachem Safe water conditioner dosing</li>
            <li><strong>New:</strong> APT Complete fertilizer (80% dose with nitrate estimation)</li>
            <li><strong>New:</strong> Hamburger menu with settings and advanced buffers</li>
            <li><strong>Improved:</strong> All calculations precision to 0.0001</li>
            <li><strong>UI:</strong> Static footer, minimalist modern design</li>
        `
    },
    kn: {
        main_title: "ಅಕ್ವೇರಿಯಂ ಕ್ಯಾಲ್ಕುಲೇಟರ್",
        subtitle: "ಡೋಸಿಂಗ್ ಮತ್ತು ನೀರಿನ ಪ್ಯಾರಾಮೀಟರ್ ವಿಶ್ಲೇಷಣೆ",
        param_status_title: "ನೀರಿನ ಪ್ಯಾರಾಮೀಟರ್ ಸ್ಥಿತಿ",
        core_params_title: "ಪ್ರಮುಖ ಪ್ಯಾರಾಮೀಟರ್‌ಗಳು",
        param_ammonia_label: "ಅಮೋನಿಯಾ (NH₃/NH₄⁺)",
        param_nitrate_label: "ನೈಟ್ರೇಟ್ (NO₃)",
        param_nitrite_label: "ನೈಟ್ರೈಟ್ (NO₂)",
        param_gh_label: "ಸಾಮಾನ್ಯ ಕಠಿಣತೆ (GH)",
        param_kh_label: "ಕಾರ್ಬೋನೇಟ್ ಕಠಿಣತೆ (KH)",
        emergency_title: "ತುರ್ತು ಶಿಫಾರಸುಗಳು",
        reco_ok: "ಯಾವುದೇ ಸಮಸ್ಯೆಗಳು ಕಂಡುಬಂದಿಲ್ಲ. ಎಲ್ಲಾ ಪ್ಯಾರಾಮೀಟರ್‌ಗಳು ಉತ್ತಮವಾಗಿವೆ!",
        reco_ammonia_detected: "ಅಮೋನಿಯಾ ಪತ್ತೆಯಾಗಿದೆ!",
        reco_prime_dose: "ವಿಷಮುಕ್ತಗೊಳಿಸಲು ತಕ್ಷಣವೇ <strong>ಸೀಕೆಮ್ ಪ್ರೈಮ್ {primeDose} ಮಿ.ಲೀ.</strong> ಡೋಸ್ ಮಾಡಿ.",
        reco_nitrite_detected: "ನೈಟ್ರೈಟ್ ಪತ್ತೆಯಾಗಿದೆ!",
        reco_stability_dose: "ಉತ್ತಮ ಬ್ಯಾಕ್ಟೀರಿಯಾವನ್ನು ಹೆಚ್ಚಿಸಲು <strong>ಸೀಕೆಮ್ ಸ್ಟೆಬಿಲಿಟಿ {stabilityDose} ಮಿ.ಲೀ.</strong> ಸೇರಿಸಿ.",
        reco_gh_low: "GH ತುಂಬಾ ಕಡಿಮೆಯಾಗಿದೆ. ಅದನ್ನು ಹೆಚ್ಚಿಸಲು <strong>ಸೀಕೆಮ್ ಇಕ್ವಿಲಿಬ್ರಿಯಂ</strong> ಸೇರಿಸುವುದನ್ನು ಪರಿಗಣಿಸಿ.",
        reco_kh_low: "KH ತುಂಬಾ ಕಡಿಮೆಯಾಗಿದೆ, ಇದು pH ಬದಲಾವಣೆಗೆ ಕಾರಣವಾಗಬಹುದು. <strong>KH ಬೂಸ್ಟರ್</strong> ಸೇರಿಸಿ.",
        reco_nitrate_high: "ಹೆಚ್ಚಿನ ನೈಟ್ರೇಟ್‌ಗಳು (>50ppm). <strong>30-50% ನೀರು ಬದಲಾವಣೆ</strong>ಯನ್ನು ಪರಿಗಣಿಸಿ.",
        reco_volume_needed: "ತುರ್ತು ಡೋಸ್‌ಗಳನ್ನು ಲೆಕ್ಕಾಚಾರ ಮಾಡಲು ನೀರಿನ ಪ್ರಮಾಣವನ್ನು ನಮೂದಿಸಿ.",
        status_good: "ಉತ್ತಮ",
        status_high: "ಹೆಚ್ಚು",
        status_danger: "ಅಪಾಯ",
        dosing_calculators_title: "ಡೋಸಿಂಗ್ ಕ್ಯಾಲ್ಕುಲೇಟರ್‌ಗಳು",
        water_volume_title: "೧. ನೀರಿನ ಪ್ರಮಾಣ",
        net_volume_label: "ನಿವ್ವಳ ನೀರಿನ ಪ್ರಮಾಣ",
        unit_label: "ಘಟಕ",
        kh_booster_title: "೨. KH ಬೂಸ್ಟರ್ (ಪೊಟ್ಯಾಸಿಯಮ್ ಬೈಕಾರ್ಬನೇಟ್)",
        gh_booster_title: "೩. GH ಬೂಸ್ಟರ್ (ಸೀಕೆಮ್ ಇಕ್ವಿಲಿಬ್ರಿಯಂ)",
        safe_title: "೪. ನೀರಿನ ಕಂಡೀಷನರ್ (ಸೀಕೆಮ್ ಸೇಫ್)",
        apt_title: "೫. ಸಸ್ಯ ರಸಗೊಬ್ಬರ (APT ಕಂಪ್ಲೀಟ್)",
        ph_neutralizer_title: "೬. pH ನ್ಯೂಟ್ರಾಲೈಜರ್ (ಸೀಕೆಮ್ ನ್ಯೂಟ್ರಲ್ ರೆಗ್ಯುಲೇಟರ್)",
        kh_reducer_title: "೭. KH ಮತ್ತು pH ರಿಡ್ಯೂಸರ್ (ಸೀಕೆಮ್ ಆಸಿಡ್ ಬಫರ್)",
        goldfish_buffer_title: "೮. ಗೋಲ್ಡ್ ಫಿಶ್ ಬಫರ್ (ಸೀಕೆಮ್ ಗೋಲ್ಡ್ ಬಫರ್)",
        current_kh_label: "ಪ್ರಸ್ತುತ KH (°dKH)",
        target_kh_label: "ಗುರಿ KH (°dKH)",
        purity_label: "ಶುದ್ಧತೆ (೦.೫-೧.೦)",
        current_gh_label: "ಪ್ರಸ್ತುತ GH (°dGH)",
        target_gh_label: "ಗುರಿ GH (°dGH)",
        current_ph_label: "ಪ್ರಸ್ತುತ pH",
        target_ph_label: "ಗುರಿ pH",
        no_dose_needed: "ಡೋಸ್ ಅಗತ್ಯವಿಲ್ಲ",
        changelog_btn: "v5.0 ಬದಲಾವಣೆ ಲಾಗ್",
        calculate_btn: "ಡೋಸ್‌ಗಳನ್ನು ಲೆಕ್ಕಾಚಾರ ಮಾಡಿ",
        download_csv_btn: "CSV ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ",
        reset_btn: "ಎಲ್ಲವನ್ನೂ ಮರುಹೊಂದಿಸಿ",
        changelog_title: "ಆವೃತ್ತಿ 5.0 ಬದಲಾವಣೆ ಲಾಗ್",
        changelog_list: `
            <li><strong>ಹೊಸ:</strong> ಅಣು ತೂಕದ ಆಧಾರದ ಮೇಲೆ KHCO3 ಲೆಕ್ಕಾಚಾರ (100.115 g/mol)</li>
            <li><strong>ಹೊಸ:</strong> L×B×H ಟ್ಯಾಂಕ್ ಆಯಾಮ ಕ್ಯಾಲ್ಕುಲೇಟರ್ (cm/in/ft)</li>
            <li><strong>ಹೊಸ:</strong> ಸೀಕೆಮ್ ಸೇಫ್ ನೀರಿನ ಕಂಡೀಷನರ್ ಡೋಸಿಂಗ್</li>
            <li><strong>ಹೊಸ:</strong> APT ಕಂಪ್ಲೀಟ್ ರಸಗೊಬ್ಬರ (80% ಡೋಸ್)</li>
            <li><strong>ಹೊಸ:</strong> ಸೆಟ್ಟಿಂಗ್‌ಗಳು ಮತ್ತು ಅಡ್ವಾನ್ಸ್ಡ್ ಬಫರ್‌ಗಳೊಂದಿಗೆ ಹ್ಯಾಂಬರ್ಗರ್ ಮೆನು</li>
            <li><strong>ಸುಧಾರಿತ:</strong> ಎಲ್ಲಾ ಲೆಕ್ಕಾಚಾರಗಳು 0.0001 ನಿಖರತೆ</li>
            <li><strong>UI:</strong> ಸ್ಥಿರ ಅಡಿಟಿಪ್ಪಣಿ, ಮಿನಿಮಲಿಸ್ಟ್ ಆಧುನಿಕ ವಿನ್ಯಾಸ</li>
        `
    }
};
