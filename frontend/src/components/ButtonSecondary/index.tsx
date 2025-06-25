import type { ReactNode } from 'react';
import './styles.css'

type Props = {
    text: ReactNode;
}

export default function ButtonSecondary({text}: Props){
    return(
        <button className='button-secondary'>{text}</button>
    );
}